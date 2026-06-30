package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.common.TopupCatalog;
import org.nhom8.banking.dto.request.ConfirmTransferRequest;
import org.nhom8.banking.dto.request.TopupInitiateRequest;
import org.nhom8.banking.dto.response.*;
import org.nhom8.banking.entity.*;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.*;
import org.nhom8.banking.service.OtpService;
import org.nhom8.banking.service.PhoneTopupService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneTopupServiceImpl implements PhoneTopupService {

    private final UserRepository            userRepository;
    private final AccountRepository         accountRepository;
    private final TransactionRepository     transactionRepository;
    private final PhoneTopupRepository      phoneTopupRepository;
    private final NotificationRepository    notificationRepository;
    private final TransferSessionRepository sessionRepository;
    private final PasswordEncoder           passwordEncoder;
    private final OtpService               otpService;

    private static final int    SESSION_TTL_MINUTES = 5;
    private static final String TRANSFER_TYPE       = "TOPUP";

    // ══════════════════════════════════════════════════════════════════════════
    // Catalog (không cần DB)
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public List<CarrierResponse> getCarriers() {
        return Arrays.stream(TopupCatalog.Carrier.values())
                .map(c -> CarrierResponse.builder()
                        .id(c.name())
                        .name(c.getDisplayName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<FaceValueResponse> getFaceValues() {
        return TopupCatalog.FACE_VALUES.stream()
                .map(fv -> FaceValueResponse.builder()
                        .amount(fv)
                        .label(formatAmount(fv))
                        .build())
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Bước 1 — Xác thực PIN, validate, lưu phiên, gửi OTP
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public TopupInitiateResponse initiateTopup(Long userId, TopupInitiateRequest req) {
        User user = verifyPin(userId, req.getPin());

        String carrierCode = req.getCarrier().toUpperCase();
        if (!TopupCatalog.isValidCarrier(carrierCode))
            throw new AppException(ErrorCode.INVALID_CARRIER);

        if (!TopupCatalog.isValidFaceValue(req.getFaceValue()))
            throw new AppException(ErrorCode.INVALID_FACE_VALUE);

        Account fromAccount = accountRepository.findById(req.getFromAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!fromAccount.getUser().getId().equals(userId))
            throw new AppException(ErrorCode.FORBIDDEN);

        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE)
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);

        if (fromAccount.getBalance().compareTo(req.getFaceValue()) < 0)
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);

        TopupCatalog.Carrier carrier = TopupCatalog.Carrier.valueOf(carrierCode);

        // Tái dùng TransferSession:
        //   toAccountNumber = số điện thoại nạp
        //   toAccountName   = carrier code ("VIETTEL", ...)
        TransferSession session = sessionRepository.save(TransferSession.builder()
                .confirmToken(UUID.randomUUID().toString())
                .user(user)
                .transferType(TRANSFER_TYPE)
                .fromAccountId(req.getFromAccountId())
                .toAccountNumber(req.getPhoneNumber())
                .toAccountName(carrierCode)
                .amount(req.getFaceValue())
                .expiresAt(LocalDateTime.now().plusMinutes(SESSION_TTL_MINUTES))
                .build());

        OtpResponse otp = otpService.generate(user, OtpCode.OtpPurpose.TRANSFER, OtpCode.OtpChannel.EMAIL);

        log.info("Topup initiated token={} userId={} carrier={} phone={} amount={}",
                session.getConfirmToken(), userId, carrierCode,
                req.getPhoneNumber(), req.getFaceValue());

        return TopupInitiateResponse.builder()
                .confirmToken(session.getConfirmToken())
                .otpResponse(otp)
                .fromAccountNumber(fromAccount.getAccountNumber())
                .fromAccountName(user.getFullName())
                .phoneNumber(req.getPhoneNumber())
                .carrier(carrierCode)
                .carrierName(carrier.getDisplayName())
                .faceValue(req.getFaceValue())
                .faceValueLabel(formatAmount(req.getFaceValue()))
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Bước 2 — Xác thực OTP, thực thi nạp tiền
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TopupReceiptResponse confirmTopup(Long userId, ConfirmTransferRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Xác thực OTP — tái dùng OtpService giống Transfer
        otpService.verify(user, req.getOtpCode(), OtpCode.OtpPurpose.TRANSFER);

        TransferSession session = sessionRepository.findByConfirmToken(req.getConfirmToken())
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId))
            throw new AppException(ErrorCode.FORBIDDEN);

        if (session.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new AppException(ErrorCode.TRANSFER_SESSION_NOT_FOUND);

        if (!TRANSFER_TYPE.equals(session.getTransferType()))
            throw new AppException(ErrorCode.TRANSFER_SESSION_NOT_FOUND);

        // Atomic mark-used — ngăn double-spend đồng thời
        int claimed = sessionRepository.markUsed(req.getConfirmToken());
        if (claimed == 0) throw new AppException(ErrorCode.TRANSFER_SESSION_NOT_FOUND);

        return executeTopup(userId, session, user);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Thực thi nạp tiền (private)
    // ══════════════════════════════════════════════════════════════════════════

    private TopupReceiptResponse executeTopup(Long userId, TransferSession session, User user) {
        // Pessimistic lock — tái dùng cùng cơ chế như Transfer
        Account fromAccount = accountRepository.findByIdForUpdate(session.getFromAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!fromAccount.getUser().getId().equals(userId))
            throw new AppException(ErrorCode.FORBIDDEN);
        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE)
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        if (fromAccount.getBalance().compareTo(session.getAmount()) < 0)
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);

        fromAccount.setBalance(fromAccount.getBalance().subtract(session.getAmount()));

        String phoneNumber  = session.getToAccountNumber();
        String carrierCode  = session.getToAccountName();
        TopupCatalog.Carrier carrier = TopupCatalog.Carrier.valueOf(carrierCode);

        String description = "Nạp " + formatAmount(session.getAmount())
                + " → " + phoneNumber + " (" + carrier.getDisplayName() + ")";

        // Ghi Transaction
        Transaction tx = Transaction.builder()
                .fromAccount(fromAccount)
                .toExternalAccount(phoneNumber)
                .toExternalAccountName(carrier.getDisplayName())
                .amount(session.getAmount())
                .fee(BigDecimal.ZERO)
                .type(Transaction.TransactionType.TOPUP)
                .status(Transaction.TransactionStatus.SUCCESS)
                .description(description)
                .referenceCode(generateReferenceCode())
                .build();
        transactionRepository.save(tx);

        // Ghi PhoneTopup (1-1 với Transaction)
        phoneTopupRepository.save(PhoneTopup.builder()
                .transaction(tx)
                .carrier(carrierCode)
                .phoneNumber(phoneNumber)
                .faceValue(session.getAmount())
                .build());

        // Notification
        notificationRepository.save(Notification.builder()
                .user(user)
                .title("Nạp tiền điện thoại thành công")
                .content("-%s → %s (%s). Số dư: %s. Mã GD: %s".formatted(
                        formatAmount(session.getAmount()), phoneNumber,
                        carrier.getDisplayName(), formatAmount(fromAccount.getBalance()),
                        tx.getReferenceCode()))
                .type(Notification.NotificationType.TRANSACTION)
                .build());

        log.info("Topup OK ref={} userId={} carrier={} phone={} amount={}",
                tx.getReferenceCode(), userId, carrierCode, phoneNumber, session.getAmount());

        return TopupReceiptResponse.builder()
                .id(tx.getId())
                .referenceCode(tx.getReferenceCode())
                .status(tx.getStatus().name())
                .fromAccountNumber(fromAccount.getAccountNumber())
                .fromAccountName(user.getFullName())
                .phoneNumber(phoneNumber)
                .carrier(carrierCode)
                .carrierName(carrier.getDisplayName())
                .faceValue(session.getAmount())
                .faceValueLabel(formatAmount(session.getAmount()))
                .description(description)
                .createdAt(tx.getCreatedAt())
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private User verifyPin(Long userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getPinHash() == null) throw new AppException(ErrorCode.PIN_NOT_SET);
        if (!passwordEncoder.matches(pin, user.getPinHash()))
            throw new AppException(ErrorCode.INVALID_PIN);
        return user;
    }

    private String generateReferenceCode() {
        String prefix = "TOP" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 5; i++) {
            String code = prefix + String.format("%07d",
                    ThreadLocalRandom.current().nextInt(10_000_000));
            if (!transactionRepository.existsByReferenceCode(code)) return code;
        }
        return "TOP" + System.nanoTime();
    }

    private String formatAmount(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,###", symbols).format(amount) + "đ";
    }
}
