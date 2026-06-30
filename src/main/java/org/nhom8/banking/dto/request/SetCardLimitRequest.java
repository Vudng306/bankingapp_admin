package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class SetCardLimitRequest {

    /** null = bỏ giới hạn hàng ngày (không giới hạn) */
    @DecimalMin(value = "0", inclusive = false, message = "Hạn mức phải lớn hơn 0")
    private BigDecimal dailyLimit;
}
