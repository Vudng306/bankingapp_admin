package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PieChartSlice {

    /** Enum key gốc: "INTERNAL", "INTERBANK", "SAVINGS_DEPOSIT", … */
    private String txKey;

    /** Nhãn tiếng Việt hiển thị trên biểu đồ */
    private String label;

    /** Tổng tiền của loại giao dịch này */
    private BigDecimal amount;

    /** Phần trăm so với tổng (làm tròn 2 chữ số) */
    private double percent;

    /** Màu hex dành cho lát cắt này, ví dụ "#3B82F6" */
    private String color;

    /** Số lượng giao dịch thuộc loại này */
    private long count;
}
