package org.nhom8.banking.service;

import org.nhom8.banking.dto.response.BarChartResponse;
import org.nhom8.banking.dto.response.PieChartResponse;
import org.nhom8.banking.dto.response.SpendingReportResponse;

import java.time.LocalDate;

public interface ReportService {

    /**
     * Thống kê thu/chi GROUP BY tuần hoặc tháng.
     *
     * @param period    "MONTH" | "WEEK"
     * @param accountId null = tất cả tài khoản của user
     */
    SpendingReportResponse getSpendingReport(Long userId, String period,
                                             LocalDate from, LocalDate to,
                                             Long accountId);

    /**
     * Dữ liệu biểu đồ CỘT (bar chart): thu / chi / dòng tiền ròng theo tuần hoặc tháng.
     * Tái sử dụng query của getSpendingReport, chỉ thay đổi shape response.
     */
    BarChartResponse getBarChart(Long userId, String period,
                                 LocalDate from, LocalDate to,
                                 Long accountId);

    /**
     * Dữ liệu biểu đồ TRÒN (pie chart): phân rã theo loại giao dịch.
     *
     * @param direction "EXPENSE" (chi) | "INCOME" (thu)
     */
    PieChartResponse getPieChart(Long userId, LocalDate from, LocalDate to,
                                 Long accountId, String direction);
}
