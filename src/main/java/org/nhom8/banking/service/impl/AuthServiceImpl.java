package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.dto.request.*;
import org.nhom8.banking.dto.response.AuthResponse;
import org.nhom8.banking.dto.response.OtpResponse;
import org.nhom8.banking.entity.Account;
import org.nhom8.banking.entity.OtpCode;
import org.nhom8.banking.entity.User;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.AccountRepository;
import org.nhom8.banking.repository.UserRepository;
import org.nhom8.banking.security.JwtTokenProvider;
import org.nhom8.banking.service.AuthService;
import org.nhom8.banking.service.DeviceService;
import org.nhom8.banking.service.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository    userRepository;
    private final AccountRepository accountRepository;
    private final OtpService        otpService;
    private final PasswordEncoder   passwordEncoder;
    private final JwtTokenProvider  jwtTokenProvider;
    private final DeviceService     deviceService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Register ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OtpResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        User user = userRepository.save(User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(User.UserStatus.LOCKED)
                .build());

        return otpService.generate(user, OtpCode.OtpPurpose.REGISTER, OtpCode.OtpChannel.EMAIL);
    }

    // ── Verify OTP ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        User user = findByCredential(request.getCredential());

        // Guard: chỉ cho phép khi user vẫn đang chờ xác thực.
        // Nếu không check, active user + resendOtp(REGISTER) + verifyOtp → account thứ 2.
        if (user.getStatus() != User.UserStatus.LOCKED) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        otpService.verify(user, request.getCode(), OtpCode.OtpPurpose.REGISTER);

        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
        createDefaultAccount(user);

        return buildAuthResponse(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional  // không readOnly: có nhánh ghi device khi client gửi deviceId
    public AuthResponse login(LoginRequest request) {
        User user = findByCredential(request.getCredential());

        if (user.getStatus() == User.UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Ghi nhận / cập nhật thiết bị nếu client cung cấp deviceId
        if (StringUtils.hasText(request.getDeviceId())) {
            RegisterDeviceRequest deviceReq = new RegisterDeviceRequest();
            deviceReq.setDeviceId(request.getDeviceId());
            deviceReq.setDeviceName(request.getDeviceName());
            deviceReq.setPushToken(request.getPushToken());
            deviceService.registerOrUpdate(user.getId(), deviceReq);
            log.info("Device recorded on login: userId={} deviceId={}", user.getId(), request.getDeviceId());
        }

        // JWT nhúng deviceId nếu có → filter sẽ kiểm tra is_active mỗi request
        return buildAuthResponse(user, request.getDeviceId());
    }

    // ── Forgot / Reset password ───────────────────────────────────────────────

    @Override
    @Transactional
    public OtpResponse forgotPassword(ForgotPasswordRequest request) {
        User user = findByCredential(request.getCredential());
        return otpService.generate(user, OtpCode.OtpPurpose.RESET_PASSWORD, OtpCode.OtpChannel.EMAIL);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = findByCredential(request.getCredential());
        otpService.verify(user, request.getOtpCode(), OtpCode.OtpPurpose.RESET_PASSWORD);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ── Resend OTP ────────────────────────────────────────────────────────────

    private static final java.util.Set<OtpCode.OtpPurpose> RESEND_ALLOWED_PURPOSES =
            java.util.Set.of(OtpCode.OtpPurpose.REGISTER, OtpCode.OtpPurpose.RESET_PASSWORD);

    @Override
    @Transactional
    public OtpResponse resendOtp(ResendOtpRequest request) {
        // Endpoint này là public — chỉ cho phép purpose liên quan đến auth.
        // TRANSFER purpose phải đi qua /transfers/internal hoặc /transfers/interbank.
        if (!RESEND_ALLOWED_PURPOSES.contains(request.getPurpose())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "purpose không hợp lệ — chỉ chấp nhận: REGISTER, RESET_PASSWORD");
        }
        User user = findByCredential(request.getCredential());
        return otpService.generate(user, request.getPurpose(), OtpCode.OtpChannel.EMAIL);
    }

    // ── Password & PIN (authenticated) ────────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void setPin(Long userId, SetPinRequest request) {
        User user = findById(userId);
        // Guard: endpoint này chỉ dành cho lần thiết lập đầu tiên.
        // Nếu đã có PIN, kẻ tấn công dùng JWT đánh cắp có thể thay PIN nạn nhân.
        if (user.getPinHash() != null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "PIN đã được thiết lập. Vui lòng dùng /auth/pin/change hoặc /profile/pin để thay đổi.");
        }
        user.setPinHash(passwordEncoder.encode(request.getPin()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePin(Long userId, ChangePinRequest request) {
        User user = findById(userId);

        if (user.getPinHash() == null) {
            throw new AppException(ErrorCode.PIN_NOT_SET);
        }
        if (!passwordEncoder.matches(request.getCurrentPin(), user.getPinHash())) {
            throw new AppException(ErrorCode.INVALID_PIN);
        }

        user.setPinHash(passwordEncoder.encode(request.getNewPin()));
        userRepository.save(user);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private User findByCredential(String credential) {
        if (credential.contains("@")) {
            return userRepository.findByEmail(credential)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }
        return userRepository.findByPhone(credential)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void createDefaultAccount(User user) {
        String accountNumber;
        do {
            long suffix = ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
            accountNumber = "9704001" + suffix;
        } while (accountRepository.existsByAccountNumber(accountNumber));

        accountRepository.save(Account.builder()
                .user(user)
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .build());
    }

    private AuthResponse buildAuthResponse(User user) {
        return buildAuthResponse(user, null);
    }

    private AuthResponse buildAuthResponse(User user, String deviceId) {
        return AuthResponse.builder()
                .token(jwtTokenProvider.generateToken(user.getId(), deviceId))
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .expiresIn(jwtExpirationMs / 1000)
                .build();
    }
}
