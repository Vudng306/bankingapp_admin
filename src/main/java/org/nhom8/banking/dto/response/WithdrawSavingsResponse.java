package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class WithdrawSavingsResponse {

    private Long savingsId;
    private String accountNumber;

    private BigDecimal principal;
    /** Lãi thực nhận (đủ hạn = fullInterest, rút sớm = demand-rate × actualDays) */
    private BigDecimal interestEarned;
    /** Tổng tiền hoàn về tài khoản = principal + interestEarned */
    private BigDecimal totalPayout;

    /** true = rút trước hạn (mất một phần lãi), false = đúng hạn / sau hạn */
    private boolean earlyWithdrawal;
    /** Chỉ có khi earlyWithdrawal=true — số ngày đã gửi thực tế */
    private Long actualDaysHeld;

    private String referenceCode;
}
