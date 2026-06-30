package org.nhom8.banking.repository;

import java.math.BigDecimal;

/**
 * Native-query projection: một hàng GROUP BY loại giao dịch.
 */
public interface TypeBreakdownProjection {
    String     getTxType();       // "INTERNAL", "INTERBANK", "SAVINGS_DEPOSIT", …
    BigDecimal getTotalAmount();  // SUM(amount)
    Long       getTxCount();      // COUNT(id)
}
