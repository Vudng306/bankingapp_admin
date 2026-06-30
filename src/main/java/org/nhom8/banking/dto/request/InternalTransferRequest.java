package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class InternalTransferRequest {

    @NotNull(message = "Tài khoản nguồn không được để trống")
    private Long fromAccountId;

    @NotBlank(message = "Số tài khoản đích không được để trống")
    private String toAccountNumber;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "1000", message = "Số tiền tối thiểu là 1,000 VND")
    @Digits(integer = 13, fraction = 2, message = "Số tiền không hợp lệ")
    private BigDecimal amount;

    @Size(max = 255, message = "Nội dung tối đa 255 ký tự")
    private String description;

    @NotBlank(message = "PIN xác nhận không được để trống")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN phải là 6 chữ số")
    private String pin;
}
