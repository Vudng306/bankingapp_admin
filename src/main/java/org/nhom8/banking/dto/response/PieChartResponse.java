package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dữ liệu sẵn cho biểu đồ tròn (pie chart) — phân tích theo loại giao dịch.
 *
 * Cách dùng trên Android (MPAndroidChart):
 *   val entries = response.slices.map { PieEntry(it.percent.toFloat(), it.label) }
 *   val colors  = response.slices.map { Color.parseColor(it.color) }
 */
@Getter
@Builder
public class PieChartResponse {

    /** "EXPENSE" (chi) hoặc "INCOME" (thu) */
    private String direction;

    private LocalDate from;
    private LocalDate to;

    /** Tổng tiền tất cả các lát cắt */
    private BigDecimal total;

    /** Các lát cắt, sắp xếp giảm dần theo amount */
    private List<PieChartSlice> slices;
}
