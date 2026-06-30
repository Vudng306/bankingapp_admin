package org.nhom8.banking.repository;

import java.math.BigDecimal;

/**
 * Native-query projection: một hàng GROUP BY tuần/tháng.
 * Tên getter phải khớp (case-insensitive) với alias của cột trong SELECT.
 */
public interface SpendingRowProjection {
    String     getLabel();    // "2024-03" hoặc "2024-W12"
    BigDecimal getIncome();   // tổng tiền vào (thu)
    BigDecimal getExpense();  // tổng tiền ra (chi)
    Long       getTxCount();  // số giao dịch
}
