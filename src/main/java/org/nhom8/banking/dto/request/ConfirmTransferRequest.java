package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ConfirmTransferRequest {

    @NotBlank(message = "Token xác nhận không được để trống")
    private String confirmToken;

    @NotBlank(message = "Mã OTP không được để trống")
    @Size(min = 6, max = 6, message = "Mã OTP phải đúng 6 ký tự")
    private String otpCode;
}
