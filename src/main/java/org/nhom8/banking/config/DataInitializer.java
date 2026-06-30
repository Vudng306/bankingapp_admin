package org.nhom8.banking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.entity.*;
import org.nhom8.banking.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final SavingsRepository savingsRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Seed data already exists, skipping.");
            return;
        }

        log.info("Seeding sample data...");

        // --- Users ---
        User user1 = userRepository.save(User.builder()
                .fullName("Nguyen Van An")
                .email("an@banking.com")
                .phone("0912345678")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .pinHash(passwordEncoder.encode("123456"))
                .build());

        User user2 = userRepository.save(User.builder()
                .fullName("Tran Thi Bich")
                .email("bich@banking.com")
                .phone("0987654321")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .pinHash(passwordEncoder.encode("123456"))
                .build());

        // --- Accounts ---
        Account acc1 = accountRepository.save(Account.builder()
                .user(user1)
                .accountNumber("9704001000000001")
                .balance(new BigDecimal("50000000.00"))
                .build());

        Account acc2 = accountRepository.save(Account.builder()
                .user(user2)
                .accountNumber("9704001000000002")
                .balance(new BigDecimal("30000000.00"))
                .build());

        // --- Transactions ---
        transactionRepository.save(Transaction.builder()
                .fromAccount(acc1)
                .toAccount(acc2)
                .amount(new BigDecimal("500000.00"))
                .type(Transaction.TransactionType.INTERNAL)
                .status(Transaction.TransactionStatus.SUCCESS)
                .description("Chia tien an trua")
                .referenceCode("REF20260101000001")
                .build());

        transactionRepository.save(Transaction.builder()
                .fromAccount(acc1)
                .toAccount(acc2)
                .amount(new BigDecimal("1000000.00"))
                .type(Transaction.TransactionType.INTERNAL)
                .status(Transaction.TransactionStatus.SUCCESS)
                .description("Tien mua do")
                .referenceCode("REF20260115000002")
                .build());

        transactionRepository.save(Transaction.builder()
                .fromAccount(acc2)
                .toAccount(acc1)
                .amount(new BigDecimal("200000.00"))
                .type(Transaction.TransactionType.INTERNAL)
                .status(Transaction.TransactionStatus.SUCCESS)
                .description("Hoan tien taxi")
                .referenceCode("REF20260120000003")
                .build());

        // --- Notifications ---
        notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Chuyển tiền thành công")
                .content("Bạn đã chuyển 500,000 VND đến TK 9704001000000002")
                .type(Notification.NotificationType.TRANSACTION)
                .build());

        notificationRepository.save(Notification.builder()
                .user(user1)
                .title("Chào mừng đến Banking App")
                .content("Tài khoản của bạn đã được kích hoạt thành công.")
                .type(Notification.NotificationType.SYSTEM)
                .read(true)
                .build());

        notificationRepository.save(Notification.builder()
                .user(user2)
                .title("Nhận tiền thành công")
                .content("Tài khoản của bạn vừa nhận 500,000 VND từ Nguyen Van An")
                .type(Notification.NotificationType.TRANSACTION)
                .build());

        // --- Savings (user1 gửi tiết kiệm 10 triệu, 12 tháng, 7.5%/năm) ---
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        savingsRepository.save(Savings.builder()
                .user(user1)
                .sourceAccount(acc1)
                .principal(new BigDecimal("10000000.00"))
                .interestRate(new BigDecimal("7.50"))
                .termMonths(12)
                .startDate(startDate)
                .maturityDate(startDate.plusMonths(12))
                .build());

        log.info("Seed data created: 2 users, 2 accounts, 3 transactions, 3 notifications, 1 savings.");
    }
}
