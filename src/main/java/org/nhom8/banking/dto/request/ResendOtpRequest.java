package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nhom8.banking.entity.OtpCode;

@Getter
@NoArgsConstructor
public class ResendOtpRequest {

    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String credential;

    @NotNull(message = "Mục đích OTP không được để trống")
    private OtpCode.OtpPurpose purpose;
}
