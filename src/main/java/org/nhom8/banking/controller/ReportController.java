package org.nhom8.banking.controller;

import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.response.BarChartResponse;
import org.nhom8.banking.dto.response.PieChartResponse;
import org.nhom8.banking.dto.response.SpendingReportResponse;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final Set<String> VALID_PERIODS    = Set.of("MONTH", "WEEK");
    private static final Set<String> VALID_DIRECTIONS = Set.of("EXPENSE", "INCOME");

    private final ReportService reportService;

    /**
     * Báo cáo thu/chi nhóm theo tuần hoặc tháng.
     *
     * GET /reports/spending
     *   ?period=MONTH          -- "MONTH" (default) | "WEEK"
     *   &from=2024-01-01       -- mặc định: 3 tháng trước
     *   &to=2024-03-31         -- mặc định: hôm nay
     *   &accountId=5           -- tuỳ chọn: lọc theo tài khoản cụ thể
     *
     * Response mẫu (MONTH):
     * {
     *   "period": "MONTH",
     *   "from": "2024-01-01",
     *   "to": "2024-03-31",
     *   "totalIncome": 15000000,
     *   "totalExpense": 8000000,
     *   "netFlow": 7000000,
     *   "series": [
     *     { "label": "2024-01", "income": 5000000, "expense": 3000000, "net": 2000000, "transactionCount": 8 },
     *     { "label": "2024-02", "income": 6000000, "expense": 2000000, "net": 4000000, "transactionCount": 5 },
     *     { "label": "2024-03", "income": 4000000, "expense": 3000000, "net": 1000000, "transactionCount": 6 }
     *   ]
     * }
     */
    @GetMapping("/spending")
    public ResponseEntity<ApiResponse<SpendingReportResponse>> getSpending(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "MONTH") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long accountId) {

        String periodUpper = period.toUpperCase();
        if (!VALID_PERIODS.contains(periodUpper))
            throw new AppException(ErrorCode.VALIDATION_ERROR);

        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusMonths(3);
        LocalDate resolvedTo   = to   != null ? to   : LocalDate.now();

        if (resolvedFrom.isAfter(resolvedTo))
            throw new AppException(ErrorCode.VALIDATION_ERROR);

        SpendingReportResponse report = reportService.getSpendingReport(
                user.getId(), periodUpper, resolvedFrom, resolvedTo, accountId);

        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    /**
     * Dữ liệu biểu đồ CỘT — thu / chi / dòng tiền ròng theo tuần/tháng.
     *
     * GET /reports/chart/bar?period=MONTH&from=2024-01-01&to=2024-03-31
     *
     * Response mẫu:
     * {
     *   "labels": ["2024-01", "2024-02", "2024-03"],
     *   "series": [
     *     { "name":"income",  "label":"Thu nhập",       "color":"#22C55E", "data":[5000000,6000000,4000000] },
     *     { "name":"expense", "label":"Chi tiêu",        "color":"#EF4444", "data":[3000000,2000000,3000000] },
     *     { "name":"net",     "label":"Dòng tiền ròng",  "color":"#3B82F6", "data":[2000000,4000000,1000000] }
     *   ]
     * }
     */
    @GetMapping("/chart/bar")
    public ResponseEntity<ApiResponse<BarChartResponse>> getBarChart(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "MONTH") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long accountId) {

        String periodUpper = period.toUpperCase();
        if (!VALID_PERIODS.contains(periodUpper))
            throw new AppException(ErrorCode.VALIDATION_ERROR);

        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusMonths(3);
        LocalDate resolvedTo   = to   != null ? to   : LocalDate.now();
        if (resolvedFrom.isAfter(resolvedTo))
            throw new AppException(ErrorCode.VALIDATION_ERROR);

        return ResponseEntity.ok(ApiResponse.ok(
                reportService.getBarChart(user.getId(), periodUpper, resolvedFrom, resolvedTo, accountId)));
    }

    /**
     * Dữ liệu biểu đồ TRÒN — phân tích chi tiêu/thu nhập theo loại giao dịch.
     *
     * GET /reports/chart/pie?direction=EXPENSE&from=2024-01-01&to=2024-03-31
     *
     * direction: EXPENSE (chi — default) | INCOME (thu)
     *
     * Response mẫu (EXPENSE):
     * {
     *   "direction": "EXPENSE",
     *   "total": 8000000,
     *   "slices": [
     *     { "txKey":"INTERBANK",       "label":"Liên ngân hàng", "amount":4000000, "percent":50.0, "color":"#F59E0B", "count":2 },
     *     { "txKey":"INTERNAL",        "label":"Chuyển khoản",   "amount":3000000, "percent":37.5, "color":"#3B82F6", "count":5 },
     *     { "txKey":"SAVINGS_DEPOSIT", "label":"Gửi tiết kiệm",  "amount":1000000, "percent":12.5, "color":"#8B5CF6", "count":1 }
     *   ]
     * }
     */
    @GetMapping("/chart/pie")
    public ResponseEntity<ApiResponse<PieChartResponse>> getPieChart(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "EXPENSE") String direction,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long accountId) {

        String directionUpper = direction.toUpperCase();
        if (!VALID_DIRECTIONS.contains(directionUpper))
            throw new AppException(ErrorCode.VALIDATION_ERROR);

        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusMonths(3);
        LocalDate resolvedTo   = to   != null ? to   : LocalDate.now();
        if (resolvedFrom.isAfter(resolvedTo))
            throw new AppException(ErrorCode.VALIDATION_ERROR);

        return ResponseEntity.ok(ApiResponse.ok(
                reportService.getPieChart(user.getId(), resolvedFrom, resolvedTo, accountId, directionUpper)));
    }
}
