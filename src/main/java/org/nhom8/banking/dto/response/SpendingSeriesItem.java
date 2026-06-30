package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SpendingSeriesItem {

    /** "2024-03" (monthly) hoặc "2024-W12" (weekly) */
    private String label;

    /** Tổng tiền nhận vào trong kỳ */
    private BigDecimal income;

    /** Tổng tiền chi ra trong kỳ */
    private BigDecimal expense;

    /** income - expense */
    private BigDecimal net;

    /** Số giao dịch trong kỳ */
    private long transactionCount;
}
