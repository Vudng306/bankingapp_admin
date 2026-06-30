package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import org.nhom8.banking.dto.response.*;
import org.nhom8.banking.entity.Account;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.AccountRepository;
import org.nhom8.banking.repository.SpendingRowProjection;
import org.nhom8.banking.repository.TransactionRepository;
import org.nhom8.banking.repository.TypeBreakdownProjection;
import org.nhom8.banking.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AccountRepository     accountRepository;
    private final TransactionRepository transactionRepository;

    // ── Màu sắc & nhãn tiếng Việt theo loại giao dịch ──────────────────────────

    private static final Map<String, String> TYPE_COLORS = Map.of(
            "INTERNAL",         "#3B82F6",   // xanh dương
            "INTERBANK",        "#F59E0B",   // cam vàng
            "SAVINGS_DEPOSIT",  "#8B5CF6",   // tím
            "SAVINGS_WITHDRAW", "#06B6D4",   // xanh lam nhạt
            "TOPUP",            "#10B981"    // xanh lá
    );

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "INTERNAL",         "Chuyển khoản",
            "INTERBANK",        "Liên ngân hàng",
            "SAVINGS_DEPOSIT",  "Gửi tiết kiệm",
            "SAVINGS_WITHDRAW", "Tất toán tiết kiệm",
            "TOPUP",            "Nạp tiền"
    );

    @Override
    @Transactional(readOnly = true)
    public SpendingReportResponse getSpendingReport(Long userId, String period,
                                                    LocalDate from, LocalDate to,
                                                    Long accountId) {
        List<Long> accountIds = resolveAccountIds(userId, accountId);

        if (accountIds.isEmpty()) {
            return emptyReport(period, from, to);
        }

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.atTime(23, 59, 59);

        List<SpendingRowProjection> rows = "WEEK".equalsIgnoreCase(period)
                ? transactionRepository.findWeeklySpending(accountIds, fromDt, toDt)
                : transactionRepository.findMonthlySpending(accountIds, fromDt, toDt);

        List<SpendingSeriesItem> series = rows.stream()
                .map(r -> {
                    BigDecimal inc = r.getIncome()  != null ? r.getIncome()  : BigDecimal.ZERO;
                    BigDecimal exp = r.getExpense() != null ? r.getExpense() : BigDecimal.ZERO;
                    return SpendingSeriesItem.builder()
                            .label(r.getLabel())
                            .income(inc)
                            .expense(exp)
                            .net(inc.subtract(exp))
                            .transactionCount(r.getTxCount() != null ? r.getTxCount() : 0L)
                            .build();
                })
                .toList();

        BigDecimal totalIncome  = series.stream()
                .map(SpendingSeriesItem::getIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = series.stream()
                .map(SpendingSeriesItem::getExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SpendingReportResponse.builder()
                .period(period.toUpperCase())
                .from(from)
                .to(to)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netFlow(totalIncome.subtract(totalExpense))
                .series(series)
                .build();
    }

    // ── Bar chart ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BarChartResponse getBarChart(Long userId, String period,
                                        LocalDate from, LocalDate to,
                                        Long accountId) {
        // Tái dùng spending-report query, chỉ đổi shape của response
        SpendingReportResponse report = getSpendingReport(userId, period, from, to, accountId);

        List<String>     labels  = report.getSeries().stream()
                .map(SpendingSeriesItem::getLabel).toList();
        List<BigDecimal> income  = report.getSeries().stream()
                .map(SpendingSeriesItem::getIncome).toList();
        List<BigDecimal> expense = report.getSeries().stream()
                .map(SpendingSeriesItem::getExpense).toList();
        List<BigDecimal> net     = report.getSeries().stream()
                .map(SpendingSeriesItem::getNet).toList();

        return BarChartResponse.builder()
                .period(period.toUpperCase())
                .from(from).to(to)
                .labels(labels)
                .series(List.of(
                        BarChartSeries.builder()
                                .name("income").label("Thu nhập")
                                .color("#22C55E").data(income).build(),
                        BarChartSeries.builder()
                                .name("expense").label("Chi tiêu")
                                .color("#EF4444").data(expense).build(),
                        BarChartSeries.builder()
                                .name("net").label("Dòng tiền ròng")
                                .color("#3B82F6").data(net).build()
                ))
                .build();
    }

    // ── Pie chart ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PieChartResponse getPieChart(Long userId, LocalDate from, LocalDate to,
                                        Long accountId, String direction) {
        List<Long> accountIds = resolveAccountIds(userId, accountId);

        if (accountIds.isEmpty()) {
            return PieChartResponse.builder()
                    .direction(direction).from(from).to(to)
                    .total(BigDecimal.ZERO).slices(List.of()).build();
        }

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.atTime(23, 59, 59);

        List<TypeBreakdownProjection> rows = "INCOME".equalsIgnoreCase(direction)
                ? transactionRepository.findIncomeBreakdownByType(accountIds, fromDt, toDt)
                : transactionRepository.findExpenseBreakdownByType(accountIds, fromDt, toDt);

        BigDecimal total = rows.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PieChartSlice> slices = rows.stream().map(r -> {
            String     key    = r.getTxType();
            BigDecimal amount = r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO;
            double percent = total.compareTo(BigDecimal.ZERO) == 0 ? 0d
                    : amount.multiply(BigDecimal.valueOf(100))
                             .divide(total, 2, RoundingMode.HALF_UP)
                             .doubleValue();
            return PieChartSlice.builder()
                    .txKey(key)
                    .label(TYPE_LABELS.getOrDefault(key, key))
                    .amount(amount)
                    .percent(percent)
                    .color(TYPE_COLORS.getOrDefault(key, "#9CA3AF"))
                    .count(r.getTxCount() != null ? r.getTxCount() : 0L)
                    .build();
        }).toList();

        return PieChartResponse.builder()
                .direction(direction.toUpperCase())
                .from(from).to(to)
                .total(total)
                .slices(slices)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Long> resolveAccountIds(Long userId, Long accountId) {
        if (accountId != null) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
            if (!account.getUser().getId().equals(userId))
                throw new AppException(ErrorCode.FORBIDDEN);
            return List.of(accountId);
        }
        return accountRepository.findByUserId(userId)
                .stream().map(Account::getId).toList();
    }

    private SpendingReportResponse emptyReport(String period, LocalDate from, LocalDate to) {
        return SpendingReportResponse.builder()
                .period(period.toUpperCase())
                .from(from).to(to)
                .totalIncome(BigDecimal.ZERO)
                .totalExpense(BigDecimal.ZERO)
                .netFlow(BigDecimal.ZERO)
                .series(List.of())
                .build();
    }
}
