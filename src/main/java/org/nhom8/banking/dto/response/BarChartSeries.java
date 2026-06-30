package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class BarChartSeries {

    /** Key nội bộ: "income" | "expense" | "net" */
    private String name;

    /** Nhãn hiển thị trên legend */
    private String label;

    /** Màu hex, ví dụ "#22C55E" */
    private String color;

    /**
     * Mảng giá trị — index tương ứng 1-1 với mảng labels của BarChartResponse.
     * Ví dụ labels=["2024-01","2024-02"], data=[5_000_000, 3_000_000]
     */
    private List<BigDecimal> data;
}
