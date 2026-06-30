package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Dữ liệu sẵn cho biểu đồ cột (bar / column chart).
 *
 * Cách dùng trên Android (MPAndroidChart):
 *   val labels = response.labels                          // X-axis
 *   val incomeEntries = response.series[0].data
 *       .mapIndexed { i, v -> BarEntry(i.toFloat(), v.toFloat()) }
 *   chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
 */
@Getter
@Builder
public class BarChartResponse {

    /** "MONTH" hoặc "WEEK" */
    private String period;

    private LocalDate from;
    private LocalDate to;

    /** Nhãn trục X: ["2024-01", "2024-02", …] hoặc ["2024-W01", …] */
    private List<String> labels;

    /**
     * Các chuỗi dữ liệu — mỗi phần tử tương ứng một màu / legend trên biểu đồ.
     * Thứ tự cố định: [income, expense, net]
     */
    private List<BarChartSeries> series;
}
