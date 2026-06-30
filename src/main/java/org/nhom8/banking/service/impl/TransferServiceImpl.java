package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.common.BankCode;
import org.nhom8.banking.dto.request.ConfirmTransferRequest;
import org.nhom8.banking.dto.request.InternalTransferRequest;
import org.nhom8.banking.dto.request.InterbankTransferRequest;
import org.nhom8.banking.dto.response.AccountLookupResponse;
import org.nhom8.banking.dto.response.OtpResponse;
import org.nhom8.banking.dto.response.TransactionResponse;
import org.nhom8.banking.dto.response.TransferInitiateResponse;
import org.nhom8.banking.entity.*;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.*;
import org.nhom8.banking.service.InterbankGatewayService;
import org.nhom8.banking.service.OtpService;
import org.nhom8.banking.service.TransferService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final UserRepository            userRepository;
    private final AccountRepository         accountRepository;
    private final TransactionRepository     transactionRepository;
    private final NotificationRepository    notificationRepository;
    private final TransferSessionRepository sessionRepository;
    private final PasswordEncoder           passwordEncoder;
    private final OtpService                otpService;
    private final InterbankGatewayService   gatewayService;

    private static final int SESSION_TTL_MINUTES = 5;

    // ══════════════════════════════════════════════════════════════════════════
    // Bước 1 — Xác thực PIN, validate, lưu phiên, gửi OTP
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public TransferInitiateResponse initiateInternal(Long userId, InternalTransferRequest req) {
        User user = verifyPin(userId, req.getPin());

        Account toAccount = accountRepository.findByAccountNumber(req.getToAccountNumber())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        Long fromId = req.getFromAccountId();
        if (fromId.equals(toAccount.getId())) throw new AppException(ErrorCode.SAME_ACCOUNT_TRANSFER);

        Account fromAccount = accountRepository.findById(fromId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateFromAccount(fromAccount, userId);
        validateActiveAccount(toAccount);
        checkBalance(fromAccount, req.getAmount());

        TransferSession session = sessionRepository.save(TransferSession.builder()
                .confirmToken(UUID.randomUUID().toString())
                .user(user)
                .transferType("INTERNAL")
                .fromAccountId(fromId)
                .toAccountNumber(req.getToAccountNumber())
                .amount(req.getAmount())
                .description(req.getDescription())
                .expiresAt(LocalDateTime.now().plusMinutes(SESSION_TTL_MINUTES))
                .build());

        OtpResponse otp = otpService.generate(user,
                OtpCode.OtpPurpose.TRANSFER, OtpCode.OtpChannel.EMAIL);

        log.info("Transfer initiated (INTERNAL) token={} userId={}", session.getConfirmToken(), userId);
        return TransferInitiateResponse.builder()
                .confirmToken(session.getConfirmToken())
                .otpResponse(otp)
                .fromAccountNumber(fromAccount.getAccountNumber())
                .fromAccountName(user.getFullName())
                .fromBankName("NHOM8 BANK")
                .toAccountNumber(toAccount.getAccountNumber())
                .toAccountName(toAccount.getUser().getFullName())
                .toBankName("NHOM8 BANK")
                .amount(req.getAmount())
                .description(req.getDescription())
                .build();
    }

    @Override
    @Transactional
    public TransferInitiateResponse initiateInterbank(Long userId, InterbankTransferRequest req) {
        User user = verifyPin(userId, req.getPin());

        BankCode bank = BankCode.fromCode(req.getToBankCode());

        Account fromAccount = accountRepository.findById(req.getFromAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateFromAccount(fromAccount, userId);
        checkBalance(fromAccount, req.getAmount());

        TransferSession session = sessionRepository.save(TransferSession.builder()
                .confirmToken(UUID.randomUUID().toString())
                .user(user)
                .transferType("INTERBANK")
                .fromAccountId(req.getFromAccountId())
                .toAccountNumber(req.getToAccountNumber())
                .toAccountName(req.getToAccountName())
                .toBankCode(bank.name())
                .amount(req.getAmount())
                .description(req.getDescription())
                .expiresAt(LocalDateTime.now().plusMinutes(SESSION_TTL_MINUTES))
                .build());

        OtpResponse otp = otpService.generate(user,
                OtpCode.OtpPurpose.TRANSFER, OtpCode.OtpChannel.EMAIL);

        log.info("Transfer initiated (INTERBANK) token={} userId={}", session.getConfirmToken(), userId);
        return TransferInitiateResponse.builder()
                .confirmToken(session.getConfirmToken())
                .otpResponse(otp)
                .fromAccountNumber(fromAccount.getAccountNumber())
                .fromAccountName(user.getFullName())
                .fromBankName("NHOM8 BANK")
                .toAccountNumber(req.getToAccountNumber())
                .toAccountName(req.getToAccountName())
                .toBankName(bank.getDisplayName())
                .amount(req.getAmount())
                .description(req.getDescription())
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Bước 2 — Xác thực OTP, thực thi giao dịch
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse confirmTransfer(Long userId, ConfirmTransferRequest req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Xác thực OTP — ném INVALID_OTP nếu sai / hết hạn
        otpService.verify(user, req.getOtpCode(), OtpCode.OtpPurpose.TRANSFER);

        // Tải và kiểm tra phiên
        TransferSession session = sessionRepository
                .findByConfirmToken(req.getConfirmToken())
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId))
            throw new AppException(ErrorCode.FORBIDDEN);

        if (session.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new AppException(ErrorCode.TRANSFER_SESSION_NOT_FOUND);

        // Atomic mark-used: UPDATE WHERE used=0 — row-level lock ngăn double-spend đồng thời.
        // Nếu một request khác đã claim phiên này trước, trả về 0 row → ném lỗi.
        int claimed = sessionRepository.markUsed(req.getConfirmToken());
        if (claimed == 0) throw new AppException(ErrorCode.TRANSFER_SESSION_NOT_FOUND);

        return switch (session.getTransferType()) {
            case "INTERNAL"  -> executeInternal(userId, session);
            case "INTERBANK" -> executeInterbank(userId, session);
            default -> throw new AppException(ErrorCode.INTERNAL_ERROR);
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Thực thi giao dịch (private)
    // ══════════════════════════════════════════════════════════════════════════

    private TransactionResponse executeInternal(Long userId, TransferSession session) {
        Account toAccount = accountRepository.findByAccountNumber(session.getToAccountNumber())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        Long fromId = session.getFromAccountId();
        Long toId   = toAccount.getId();

        // Tái kiểm tra — số dư có thể đã thay đổi từ lúc initiate
        Account[] locked    = lockOrdered(fromId, toId);
        Account fromAccount = locked[0].getId().equals(fromId) ? locked[0] : locked[1];
        toAccount           = locked[0].getId().equals(toId)   ? locked[0] : locked[1];

        validateFromAccount(fromAccount, userId);
        validateActiveAccount(toAccount);
        checkBalance(fromAccount, session.getAmount());

        fromAccount.setBalance(fromAccount.getBalance().subtract(session.getAmount()));
        toAccount  .setBalance(toAccount  .getBalance().add     (session.getAmount()));

        Transaction tx = Transaction.builder()
                .fromAccount(fromAccount).toAccount(toAccount)
                .amount(session.getAmount()).fee(BigDecimal.ZERO)
                .type(Transaction.TransactionType.INTERNAL)
                .status(Transaction.TransactionStatus.SUCCESS)
                .description(session.getDescription())
                .referenceCode(generateReferenceCode())
                .build();
        transactionRepository.save(tx);

        notificationRepository.saveAll(List.of(
                notif(fromAccount.getUser(), "Chuyển tiền thành công",
                        "-%,.0f VND → TK %s. Số dư: %,.0f VND. Mã GD: %s"
                                .formatted(session.getAmount(), toAccount.getAccountNumber(),
                                           fromAccount.getBalance(), tx.getReferenceCode())),
                notif(toAccount.getUser(), "Nhận tiền thành công",
                        "+%,.0f VND từ TK %s. Số dư: %,.0f VND. Mã GD: %s"
                                .formatted(session.getAmount(), fromAccount.getAccountNumber(),
                                           toAccount.getBalance(), tx.getReferenceCode()))
        ));

        log.info("Internal OK ref={} from={} to={} amount={}",
                tx.getReferenceCode(), fromId, toId, session.getAmount());
        return toResponse(tx);
    }

    private TransactionResponse executeInterbank(Long userId, TransferSession session) {
        BankCode bank = BankCode.fromCode(session.getToBankCode());

        Account fromAccount = accountRepository.findByIdForUpdate(session.getFromAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateFromAccount(fromAccount, userId);
        checkBalance(fromAccount, session.getAmount());

        fromAccount.setBalance(fromAccount.getBalance().subtract(session.getAmount()));

        String desc = buildInterbankDesc(session, bank);

        Transaction tx = Transaction.builder()
                .fromAccount(fromAccount)
                .toExternalAccount(session.getToAccountNumber())
                .toExternalAccountName(session.getToAccountName())
                .toBankCode(bank.name())
                .amount(session.getAmount()).fee(BigDecimal.ZERO)
                .type(Transaction.TransactionType.INTERBANK)
                .status(Transaction.TransactionStatus.PENDING)
                .description(desc)
                .referenceCode(generateReferenceCode())
                .build();
        transactionRepository.save(tx);

        notificationRepository.save(notif(fromAccount.getUser(),
                "Lệnh chuyển tiền đang xử lý",
                "-%,.0f VND → TK %s (%s) tại %s. Số dư: %,.0f VND. Mã GD: %s"
                        .formatted(session.getAmount(), session.getToAccountNumber(),
                                session.getToAccountName(), bank.getDisplayName(),
                                fromAccount.getBalance(), tx.getReferenceCode())));

        gatewayService.dispatch(tx.getId(), session.getFromAccountId(), session.getAmount());

        log.info("Interbank PENDING ref={} from={} to={}@{} amount={}",
                tx.getReferenceCode(), session.getFromAccountId(),
                session.getToAccountNumber(), bank.name(), session.getAmount());
        return toResponse(tx);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Lookup
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public AccountLookupResponse lookupAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        return AccountLookupResponse.builder()
                .accountNumber(account.getAccountNumber())
                .accountName(account.getUser().getFullName())
                .bankName("NHOM8 BANK")
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Xác thực PIN, trả về User để dùng tiếp (gửi OTP, lưu session) */
    private User verifyPin(Long userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getPinHash() == null) throw new AppException(ErrorCode.PIN_NOT_SET);
        if (!passwordEncoder.matches(pin, user.getPinHash())) throw new AppException(ErrorCode.INVALID_PIN);
        return user;
    }

    /** Lock 2 account theo thứ tự ID tăng dần — tránh deadlock */
    private Account[] lockOrdered(Long idA, Long idB) {
        Long first  = Math.min(idA, idB);
        Long second = Math.max(idA, idB);
        Account a = accountRepository.findByIdForUpdate(first)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account b = accountRepository.findByIdForUpdate(second)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        return new Account[]{ a, b };
    }

    private void validateFromAccount(Account account, Long userId) {
        if (!account.getUser().getId().equals(userId)) throw new AppException(ErrorCode.FORBIDDEN);
        validateActiveAccount(account);
    }

    private void validateActiveAccount(Account account) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
    }

    private void checkBalance(Account account, BigDecimal required) {
        if (account.getBalance().compareTo(required) < 0)
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
    }

    private String generateReferenceCode() {
        String prefix = "TXN" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 5; i++) {
            String code = prefix + String.format("%08d",
                    ThreadLocalRandom.current().nextInt(100_000_000));
            if (!transactionRepository.existsByReferenceCode(code)) return code;
        }
        return "TXN" + System.nanoTime();
    }

    private TransactionResponse toResponse(Transaction tx) {
        boolean isInterbank = tx.getType() == Transaction.TransactionType.INTERBANK;
        return TransactionResponse.builder()
                .id(tx.getId()).type(tx.getType().name()).status(tx.getStatus().name())
                .amount(tx.getAmount()).fee(tx.getFee()).direction("DEBIT")
                .counterpartAccount(isInterbank ? tx.getToExternalAccount()
                                                : tx.getToAccount().getAccountNumber())
                .counterpartBank(tx.getToBankCode())
                .description(tx.getDescription()).referenceCode(tx.getReferenceCode())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private Notification notif(User user, String title, String content) {
        return Notification.builder().user(user).title(title).content(content)
                .type(Notification.NotificationType.TRANSACTION).build();
    }

    private String buildInterbankDesc(TransferSession s, BankCode bank) {
        String base = "CK den " + s.getToAccountName()
                + " - " + bank.getDisplayName()
                + " (" + s.getToAccountNumber() + ")";
        return (s.getDescription() != null && !s.getDescription().isBlank())
                ? s.getDescription() + " | " + base : base;
    }
}
