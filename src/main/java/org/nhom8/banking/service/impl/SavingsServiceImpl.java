package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.dto.request.OpenSavingsRequest;
import org.nhom8.banking.dto.request.WithdrawSavingsRequest;
import org.nhom8.banking.dto.response.SavingsResponse;
import org.nhom8.banking.dto.response.WithdrawSavingsResponse;
import org.nhom8.banking.entity.*;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.*;
import org.nhom8.banking.service.SavingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsServiceImpl implements SavingsService {

    private final UserRepository         userRepository;
    private final AccountRepository      accountRepository;
    private final SavingsRepository      savingsRepository;
    private final TransactionRepository  transactionRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder        passwordEncoder;

    @Value("${app.savings.min-amount:1000000}")
    private BigDecimal minAmount;

    /** Lãi suất không kỳ hạn (%/năm) — áp dụng khi rút trước ngày đáo hạn */
    @Value("${app.savings.demand-rate:0.50}")
    private BigDecimal demandRate;

    /** Lãi suất năm (%) theo kỳ hạn tháng */
    private static final Map<Integer, BigDecimal> INTEREST_RATES = Map.of(
            1,  new BigDecimal("3.50"),
            3,  new BigDecimal("4.50"),
            6,  new BigDecimal("5.50"),
            12, new BigDecimal("6.50"),
            24, new BigDecimal("7.00")
    );

    private static final BigDecimal HUNDRED      = BigDecimal.valueOf(100);
    private static final BigDecimal DAYS_IN_YEAR = BigDecimal.valueOf(365);

    // ══════════════════════════════════════════════════════════════════════════
    // Mở sổ
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SavingsResponse open(Long userId, OpenSavingsRequest request) {

        User    user    = verifyPin(userId, request.getPin());
        Account account = lockAccount(request.getFromAccountId(), userId);

        BigDecimal annualRate = resolveRate(request.getTermMonths());

        if (request.getAmount().compareTo(minAmount) < 0)
            throw new AppException(ErrorCode.SAVINGS_MINIMUM_AMOUNT);
        if (account.getBalance().compareTo(request.getAmount()) < 0)
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);

        account.setBalance(account.getBalance().subtract(request.getAmount()));

        LocalDate startDate    = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(request.getTermMonths());

        // accruedInterest bắt đầu = 0; dailyAccrualJob cộng từng ngày
        Savings savings = savingsRepository.save(Savings.builder()
                .user(user)
                .sourceAccount(account)
                .principal(request.getAmount())
                .interestRate(annualRate)
                .termMonths(request.getTermMonths())
                .startDate(startDate)
                .maturityDate(maturityDate)
                .accruedInterest(BigDecimal.ZERO)
                .status(Savings.SavingsStatus.ACTIVE)
                .build());

        BigDecimal expectedInterest = calcTermInterest(
                request.getAmount(), annualRate, request.getTermMonths());

        String refCode = generateReferenceCode("SAV");
        transactionRepository.save(Transaction.builder()
                .fromAccount(account)
                .amount(request.getAmount())
                .fee(BigDecimal.ZERO)
                .type(Transaction.TransactionType.SAVINGS_DEPOSIT)
                .status(Transaction.TransactionStatus.SUCCESS)
                .description("Gửi tiết kiệm %d tháng - lãi suất %.2f%%/năm"
                        .formatted(request.getTermMonths(), annualRate))
                .referenceCode(refCode)
                .build());

        notificationRepository.save(buildNotif(user,
                "Mở sổ tiết kiệm thành công",
                "-%,.0f VND. Kỳ hạn %d tháng (%.2f%%/năm), đáo hạn %s, nhận dự kiến %,.0f VND. Số dư TK: %,.0f VND. Mã: %s"
                        .formatted(request.getAmount(), request.getTermMonths(), annualRate,
                                   maturityDate, request.getAmount().add(expectedInterest),
                                   account.getBalance(), refCode)));

        log.info("Savings opened id={} userId={} amount={} term={}m expectedInterest={} ref={}",
                savings.getId(), userId, request.getAmount(),
                request.getTermMonths(), expectedInterest, refCode);
        return toResponse(savings);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tất toán (rút sổ)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Tất toán sổ tiết kiệm.
     *
     * Quy tắc payout — tính FRESH, không dùng stored accruedInterest để tránh
     * lệch timing với job chưa chạy:
     *
     *   Đúng hạn / quá hạn (maturityDate ≤ today):
     *     interest = calcTermInterest(principal, annualRate, termMonths)   ← lãi hợp đồng
     *     payout   = principal + interest
     *
     *   Rút sớm (maturityDate > today):
     *     interest = calcDayInterest(principal, demandRate, actualDays)    ← lãi không kỳ hạn
     *     payout   = principal + interest
     *
     * Flow an toàn với concurrent:
     *   PIN → load savings → tính payout → lock account (FOR UPDATE) → cộng balance
     *   → savings.WITHDRAWN → Transaction(SAVINGS_WITHDRAW) → Notification
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public WithdrawSavingsResponse withdraw(Long userId, Long savingsId,
                                            WithdrawSavingsRequest request) {
        verifyPin(userId, request.getPin());

        Savings savings = savingsRepository.findByIdAndUserId(savingsId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.SAVINGS_NOT_FOUND));

        if (savings.getStatus() == Savings.SavingsStatus.WITHDRAWN)
            throw new AppException(ErrorCode.SAVINGS_ALREADY_CLOSED);

        LocalDate today = LocalDate.now();
        boolean   early = savings.getMaturityDate().isAfter(today);

        BigDecimal interestEarned;
        Long       actualDays = null;

        if (early) {
            actualDays     = today.toEpochDay() - savings.getStartDate().toEpochDay();
            interestEarned = calcDayInterest(savings.getPrincipal(), demandRate, actualDays);
        } else {
            // Đúng hạn: dùng công thức kỳ hạn — nhất quán với "hợp đồng" khi mở sổ
            interestEarned = calcTermInterest(savings.getPrincipal(),
                                              savings.getInterestRate(),
                                              savings.getTermMonths());
        }

        BigDecimal payout = savings.getPrincipal().add(interestEarned);

        // Lock account (FOR UPDATE) trước khi cộng số dư
        Account account = accountRepository.findByIdForUpdate(savings.getSourceAccount().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);

        account.setBalance(account.getBalance().add(payout));

        // Cố định accruedInterest = lãi thực nhận (audit trail)
        savings.setAccruedInterest(interestEarned);
        savings.setStatus(Savings.SavingsStatus.WITHDRAWN);

        String refCode = generateReferenceCode("WDR");
        transactionRepository.save(Transaction.builder()
                .toAccount(account)
                .amount(payout)
                .fee(BigDecimal.ZERO)
                .type(Transaction.TransactionType.SAVINGS_WITHDRAW)
                .status(Transaction.TransactionStatus.SUCCESS)
                .description(early
                        ? "Tất toán sớm sổ TK #%d (gửi %d ngày, lãi suất không kỳ hạn %.2f%%/năm)"
                                .formatted(savingsId, actualDays, demandRate)
                        : "Tất toán đúng hạn sổ TK #%d".formatted(savingsId))
                .referenceCode(refCode)
                .build());

        notificationRepository.save(buildNotif(savings.getUser(),
                early ? "Tất toán sớm sổ tiết kiệm" : "Tất toán sổ tiết kiệm thành công",
                "+%,.0f VND (gốc %,.0f + lãi %,.0f) vào TK %s. Số dư: %,.0f VND. Mã: %s"
                        .formatted(payout, savings.getPrincipal(),
                                   interestEarned, account.getAccountNumber(),
                                   account.getBalance(), refCode)));

        log.info("Savings withdrawn id={} userId={} early={} interestEarned={} payout={} ref={}",
                savingsId, userId, early, interestEarned, payout, refCode);

        return WithdrawSavingsResponse.builder()
                .savingsId(savingsId)
                .accountNumber(account.getAccountNumber())
                .principal(savings.getPrincipal())
                .interestEarned(interestEarned)
                .totalPayout(payout)
                .earlyWithdrawal(early)
                .actualDaysHeld(actualDays)
                .referenceCode(refCode)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Đọc
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<SavingsResponse> list(Long userId) {
        return savingsRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SavingsResponse detail(Long userId, Long savingsId) {
        return toResponse(savingsRepository.findByIdAndUserId(savingsId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.SAVINGS_NOT_FOUND)));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Scheduled jobs
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Cộng lãi hàng ngày vào accruedInterest của tất cả sổ ACTIVE.
     *
     * Chạy lúc 00:00 mỗi ngày.
     * Công thức: dailyInterest = principal × (annualRate/100) / 365
     *
     * accruedInterest chỉ dùng để HIỂN THỊ "lãi đang chạy" cho user.
     * Payout khi tất toán được tính độc lập trong withdraw() để tránh
     * phụ thuộc vào timing của job này.
     *
     * @Transactional hoạt động vì Spring scheduler gọi qua bean proxy.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void dailyInterestAccrualJob() {
        List<Savings> active = savingsRepository.findByStatus(Savings.SavingsStatus.ACTIVE);
        if (active.isEmpty()) return;

        for (Savings s : active) {
            // Chỉ tích lũy trong phạm vi kỳ hạn (tránh cộng quá sau ngày đáo hạn)
            if (LocalDate.now().isAfter(s.getMaturityDate())) continue;

            BigDecimal dailyInterest = calcDayInterest(s.getPrincipal(), s.getInterestRate(), 1);
            s.setAccruedInterest(s.getAccruedInterest().add(dailyInterest));
        }

        log.info("dailyInterestAccrualJob: processed {} active savings", active.size());
    }

    /**
     * Tự động chuyển ACTIVE → MATURED cho sổ đã qua ngày đáo hạn.
     *
     * Chạy lúc 01:05 mỗi ngày — sau dailyAccrualJob để accruedInterest
     * đã được cập nhật đầy đủ trước khi gửi thông báo.
     */
    @Scheduled(cron = "0 5 1 * * *")
    @Transactional
    public void markMaturedJob() {
        List<Savings> due = savingsRepository.findByStatusAndMaturityDateLessThanEqual(
                Savings.SavingsStatus.ACTIVE, LocalDate.now());
        if (due.isEmpty()) return;

        for (Savings s : due) {
            s.setStatus(Savings.SavingsStatus.MATURED);

            BigDecimal fullInterest  = calcTermInterest(s.getPrincipal(),
                                           s.getInterestRate(), s.getTermMonths());
            BigDecimal fullPayout    = s.getPrincipal().add(fullInterest);

            notificationRepository.save(buildNotif(s.getUser(),
                    "Sổ tiết kiệm đã đáo hạn",
                    "Sổ TK %d tháng (%,.0f VND) đã đến ngày đáo hạn. Vui lòng tất toán để nhận %,.0f VND."
                            .formatted(s.getTermMonths(), s.getPrincipal(), fullPayout)));
        }

        log.info("markMaturedJob: {} savings marked MATURED", due.size());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tính lãi — pure static, không có side effect, dễ unit test
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Đơn lãi theo kỳ hạn tháng (dùng cho hợp đồng + payout đúng hạn):
     *   I = P × (r/100) × termMonths/12
     */
    static BigDecimal calcTermInterest(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        return principal
                .multiply(annualRate.divide(HUNDRED, 10, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(termMonths))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    /**
     * Đơn lãi theo ngày (dùng cho tích lũy hàng ngày + rút sớm):
     *   I = P × (r/100) × days/365
     *
     * Dùng 365 thay vì 360 (thông lệ ngân hàng Việt Nam).
     */
    static BigDecimal calcDayInterest(BigDecimal principal, BigDecimal annualRate, long days) {
        if (days <= 0) return BigDecimal.ZERO;
        return principal
                .multiply(annualRate.divide(HUNDRED, 10, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(days))
                .divide(DAYS_IN_YEAR, 2, RoundingMode.HALF_UP);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private User verifyPin(Long userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getPinHash() == null) throw new AppException(ErrorCode.PIN_NOT_SET);
        if (!passwordEncoder.matches(pin, user.getPinHash())) throw new AppException(ErrorCode.INVALID_PIN);
        return user;
    }

    private Account lockAccount(Long accountId, Long userId) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!account.getUser().getId().equals(userId))
            throw new AppException(ErrorCode.FORBIDDEN);
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        return account;
    }

    private BigDecimal resolveRate(Integer termMonths) {
        BigDecimal rate = INTEREST_RATES.get(termMonths);
        if (rate == null) throw new AppException(ErrorCode.SAVINGS_INVALID_TERM);
        return rate;
    }

    private String generateReferenceCode(String prefix) {
        String daily = prefix + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 5; i++) {
            String code = daily + String.format("%08d",
                    ThreadLocalRandom.current().nextInt(100_000_000));
            if (!transactionRepository.existsByReferenceCode(code)) return code;
        }
        return prefix + System.nanoTime();
    }

    private Notification buildNotif(User user, String title, String content) {
        return Notification.builder()
                .user(user).title(title).content(content)
                .type(Notification.NotificationType.TRANSACTION)
                .build();
    }

    private SavingsResponse toResponse(Savings s) {
        LocalDate  today          = LocalDate.now();
        long       daysRemaining  = Math.max(0, s.getMaturityDate().toEpochDay() - today.toEpochDay());
        boolean    matured        = !s.getMaturityDate().isAfter(today);

        // Lãi dự kiến đủ hạn (bất biến — tính từ công thức kỳ hạn)
        BigDecimal expectedInterest = calcTermInterest(
                s.getPrincipal(), s.getInterestRate(), s.getTermMonths());

        // Preview rút sớm: lãi không kỳ hạn × ngày đã gửi (chỉ dùng khi ACTIVE chưa đáo hạn)
        long       daysHeld              = Math.max(0, today.toEpochDay() - s.getStartDate().toEpochDay());
        BigDecimal earlyWithdrawInterest = calcDayInterest(s.getPrincipal(), demandRate, daysHeld);
        BigDecimal earlyWithdrawPayout   = s.getPrincipal().add(earlyWithdrawInterest);

        return SavingsResponse.builder()
                .id(s.getId())
                .sourceAccountNumber(s.getSourceAccount().getAccountNumber())
                .principal(s.getPrincipal())
                .interestRate(s.getInterestRate())
                .termMonths(s.getTermMonths())
                .startDate(s.getStartDate())
                .maturityDate(s.getMaturityDate())
                // accruedInterest = lãi tích lũy thực tế từ daily job (hiển thị tiến trình)
                .accruedInterest(s.getAccruedInterest())
                // expectedInterest / expectedPayout = hợp đồng, không đổi
                .expectedInterest(expectedInterest)
                .expectedPayout(s.getPrincipal().add(expectedInterest))
                .earlyWithdrawInterest(earlyWithdrawInterest)
                .earlyWithdrawPayout(earlyWithdrawPayout)
                .status(s.getStatus().name())
                .matured(matured)
                .daysRemaining(daysRemaining)
                .createdAt(s.getCreatedAt())
                .build();
    }
}
