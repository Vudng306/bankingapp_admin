package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class TopupInitiateRequest {

    @NotNull(message = "Vui lòng chọn tài khoản nguồn")
    private Long fromAccountId;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(
        regexp = "^(0[3-9][0-9]{8}|\\+84[3-9][0-9]{8})$",
        message = "Số điện thoại không đúng định dạng (VD: 0912345678)"
    )
    private String phoneNumber;

    @NotBlank(message = "Vui lòng chọn nhà mạng")
    private String carrier;

    @NotNull(message = "Vui lòng chọn mệnh giá")
    @DecimalMin(value = "10000", message = "Mệnh giá tối thiểu là 10.000đ")
    private BigDecimal faceValue;

    @NotBlank(message = "Vui lòng nhập mã PIN")
    private String pin;
}
