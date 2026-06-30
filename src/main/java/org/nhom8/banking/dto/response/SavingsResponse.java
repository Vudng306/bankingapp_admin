package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SavingsResponse {

    private Long id;
    private String sourceAccountNumber;

    private BigDecimal principal;
    /** Lãi suất năm (%), ví dụ 6.50 */
    private BigDecimal interestRate;
    private Integer termMonths;

    private LocalDate startDate;
    private LocalDate maturityDate;

    // ── Lãi tích lũy (cập nhật hàng ngày bởi dailyAccrualJob) ───────────────
    /**
     * Lãi đã tích lũy thực tế tính đến hôm nay.
     * Bắt đầu = 0 khi mở sổ, cộng thêm mỗi ngày đến khi đáo hạn.
     * Dùng để hiển thị "lãi đang chạy" cho user.
     */
    private BigDecimal accruedInterest;

    // ── Dự kiến khi đáo hạn (tính theo công thức kỳ hạn, không đổi) ─────────
    /**
     * Lãi dự kiến khi đủ kỳ hạn = principal × rate/100 × termMonths/12.
     * Không đổi trong suốt vòng đời sổ — là "hợp đồng" ban đầu.
     */
    private BigDecimal expectedInterest;
    /** Tổng nhận khi đáo hạn = principal + expectedInterest */
    private BigDecimal expectedPayout;

    // ── Rút sớm (preview realtime) ───────────────────────────────────────────
    /** Lãi nhận nếu rút HÔM NAY (lãi suất không kỳ hạn × số ngày đã gửi) */
    private BigDecimal earlyWithdrawInterest;
    /** Tổng nhận nếu rút hôm nay */
    private BigDecimal earlyWithdrawPayout;

    // ── Trạng thái ───────────────────────────────────────────────────────────
    private String status;
    /** true nếu đã đến hoặc quá ngày đáo hạn */
    private boolean matured;
    /** Số ngày còn lại (0 nếu đã đáo hạn / đã tất toán) */
    private long daysRemaining;

    private LocalDateTime createdAt;
}
