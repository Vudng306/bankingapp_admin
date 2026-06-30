package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String credential;

    @NotBlank(message = "Mã OTP không được để trống")
    @Size(min = 6, max = 6, message = "Mã OTP phải đúng 6 ký tự")
    private String otpCode;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8–100 ký tự")
    private String newPassword;
}
