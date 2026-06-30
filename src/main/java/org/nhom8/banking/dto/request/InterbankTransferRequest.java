package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class InterbankTransferRequest {

    @NotNull(message = "Tài khoản nguồn không được để trống")
    private Long fromAccountId;

    @NotBlank(message = "Mã ngân hàng không được để trống")
    private String toBankCode;           // VCB, TCB, MB, ...

    @NotBlank(message = "Số tài khoản đích không được để trống")
    private String toAccountNumber;      // số tài khoản ở ngân hàng đích

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự")
    private String toAccountName;        // tên chủ tài khoản đích (hiển thị)

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "10000", message = "Số tiền liên ngân hàng tối thiểu 10,000 VND")
    @Digits(integer = 13, fraction = 2, message = "Số tiền không hợp lệ")
    private BigDecimal amount;

    @Size(max = 255, message = "Nội dung tối đa 255 ký tự")
    private String description;

    @NotBlank(message = "PIN xác nhận không được để trống")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN phải là 6 chữ số")
    private String pin;
}
