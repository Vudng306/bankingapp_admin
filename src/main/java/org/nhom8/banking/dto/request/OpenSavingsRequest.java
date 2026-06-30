package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OpenSavingsRequest {

    @NotNull(message = "fromAccountId là bắt buộc")
    private Long fromAccountId;

    /** Số tiền gửi — validate tối thiểu thêm trong service theo config */
    @NotNull(message = "amount là bắt buộc")
    @DecimalMin(value = "0.01", message = "amount phải > 0")
    private BigDecimal amount;

    /** Kỳ hạn tháng — phải là một trong {1, 3, 6, 12, 24} */
    @NotNull(message = "termMonths là bắt buộc")
    private Integer termMonths;

    @NotBlank(message = "pin là bắt buộc")
    private String pin;
}
