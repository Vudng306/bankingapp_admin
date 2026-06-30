package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SpendingReportResponse {

    /** "MONTH" hoặc "WEEK" */
    private String period;

    private LocalDate from;
    private LocalDate to;

    /** Tổng thu trong toàn bộ khoảng thời gian */
    private BigDecimal totalIncome;

    /** Tổng chi trong toàn bộ khoảng thời gian */
    private BigDecimal totalExpense;

    /** totalIncome - totalExpense */
    private BigDecimal netFlow;

    /** Chuỗi thời gian: mỗi phần tử là một tuần/tháng */
    private List<SpendingSeriesItem> series;
}
