package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.entity.Account;
import org.nhom8.banking.entity.Notification;
import org.nhom8.banking.entity.Transaction;
import org.nhom8.banking.entity.User;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.AccountRepository;
import org.nhom8.banking.repository.NotificationRepository;
import org.nhom8.banking.repository.TransactionRepository;
import org.nhom8.banking.service.InterbankGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterbankGatewayServiceImpl implements InterbankGatewayService {

    private final TransactionRepository  transactionRepository;
    private final AccountRepository      accountRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Self-injection qua interface để @Transactional trong settle() đi qua Spring proxy.
     * Gọi this.settle() trực tiếp sẽ bypass proxy → transaction không được mở.
     */
    @Autowired
    @Lazy
    private InterbankGatewayService self;

    @Value("${app.interbank.success-rate:85}")
    private int successRatePercent;

    @Value("${app.interbank.delay-min-ms:3000}")
    private int delayMinMs;

    @Value("${app.interbank.delay-max-ms:8000}")
    private int delayMaxMs;

    // ── dispatch — @Async, không giữ DB connection trong lúc sleep ────────────

    @Override
    @Async("transferExecutor")
    public void dispatch(Long transactionId, Long fromAccountId, BigDecimal amount) {
        try {
            int delayMs = ThreadLocalRandom.current().nextInt(delayMinMs, delayMaxMs + 1);
            log.info("[INTERBANK] txId={} dispatched, simulating {}ms delay", transactionId, delayMs);
            Thread.sleep(delayMs);

            boolean success = ThreadLocalRandom.current().nextInt(100) < successRatePercent;
            log.info("[INTERBANK] txId={} result={}", transactionId, success ? "SUCCESS" : "FAILED");

            // Gọi qua self-proxy để @Transactional được áp dụng
            self.settle(transactionId, fromAccountId, amount, success);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[INTERBANK] txId={} interrupted, attempting FAILED settlement", transactionId);
            self.settle(transactionId, fromAccountId, amount, false);
        }
    }

    // ── settle — @Transactional riêng, chạy sau khi sleep kết thúc ──────────

    @Override
    @Transactional
    public void settle(Long transactionId, Long fromAccountId, BigDecimal amount, boolean success) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (tx.getStatus() != Transaction.TransactionStatus.PENDING) {
            log.warn("[INTERBANK] txId={} already settled, skipping", transactionId);
            return; // idempotent guard
        }

        User user;
        BigDecimal balanceAfter;

        if (success) {
            tx.setStatus(Transaction.TransactionStatus.SUCCESS);
            Account fromAccount = accountRepository.findById(fromAccountId)
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
            user        = fromAccount.getUser();
            balanceAfter = fromAccount.getBalance();

        } else {
            tx.setStatus(Transaction.TransactionStatus.FAILED);
            // Hoàn tiền: cần pessimistic lock để đảm bảo an toàn
            Account fromAccount = accountRepository.findByIdForUpdate(fromAccountId)
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
            fromAccount.setBalance(fromAccount.getBalance().add(amount));
            user        = fromAccount.getUser();
            balanceAfter = fromAccount.getBalance();
            log.info("[INTERBANK] txId={} FAILED → refunded {} to accountId={}",
                    transactionId, amount, fromAccountId);
        }

        notificationRepository.save(buildNotification(user, tx, amount, success, balanceAfter));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Notification buildNotification(User user, Transaction tx,
                                           BigDecimal amount, boolean success,
                                           BigDecimal balanceAfter) {
        String title, content;
        if (success) {
            title = "Chuyển tiền liên ngân hàng thành công";
            String toName = tx.getToExternalAccountName() != null
                    ? " (" + tx.getToExternalAccountName() + ")" : "";
            content = String.format(
                    "-%,.0f VND → TK %s%s tại %s. Số dư: %,.0f VND. Mã GD: %s",
                    amount, tx.getToExternalAccount(), toName, tx.getToBankCode(),
                    balanceAfter, tx.getReferenceCode());
        } else {
            title = "Chuyển tiền liên ngân hàng thất bại";
            content = String.format(
                    "Giao dịch %s thất bại. Hoàn %,.0f VND. Số dư: %,.0f VND.",
                    tx.getReferenceCode(), amount, balanceAfter);
        }

        return Notification.builder()
                .user(user)
                .title(title)
                .content(content)
                .type(Notification.NotificationType.TRANSACTION)
                .build();
    }
}
