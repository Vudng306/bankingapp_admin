package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.dto.response.OtpResponse;
import org.nhom8.banking.entity.OtpCode;
import org.nhom8.banking.entity.User;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.OtpCodeRepository;
import org.nhom8.banking.service.EmailService;
import org.nhom8.banking.service.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final EmailService       emailService;

    @Value("${app.otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${app.otp.dev-mode:true}")
    private boolean devMode;

    // ── Generate ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OtpResponse generate(User user, OtpCode.OtpPurpose purpose, OtpCode.OtpChannel channel) {
        otpCodeRepository.invalidatePrevious(user.getId(), purpose);

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

        otpCodeRepository.save(OtpCode.builder()
                .user(user)
                .code(code)
                .purpose(purpose)
                .channel(channel)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build());

        if (devMode) {
            // Fake mode: in log, trả code trong response
            log.info("[OTP-DEV] purpose={} email={} code={}", purpose, user.getEmail(), code);
        } else {
            // Production: gửi email thật (async — không block request)
            if (channel == OtpCode.OtpChannel.EMAIL) {
                emailService.sendOtpEmail(
                        user.getEmail(), user.getFullName(), code, purpose, expiryMinutes);
            }
            // SMS channel: tích hợp Twilio sau
        }

        String target = channel == OtpCode.OtpChannel.EMAIL
                ? maskEmail(user.getEmail())
                : maskPhone(user.getPhone());

        return OtpResponse.builder()
                .channel(channel.name())
                .target(target)
                .expiresInSeconds(expiryMinutes * 60)
                .devOtpCode(devMode ? code : null)
                .build();
    }

    // ── Verify ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void verify(User user, String inputCode, OtpCode.OtpPurpose purpose) {
        OtpCode otp = otpCodeRepository
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }
        if (!otp.getCode().equals(inputCode)) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        otp.setUsed(true);
        otpCodeRepository.save(otp);
    }

    // ── Mask helpers ─────────────────────────────────────────────────────────

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at < 2) return email;
        return email.substring(0, 2) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone.length() < 7) return phone;
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 4);
    }
}
