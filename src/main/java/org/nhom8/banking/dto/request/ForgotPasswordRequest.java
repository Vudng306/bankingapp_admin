package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ForgotPasswordRequest {

    /** Email hoặc số điện thoại */
    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String credential;
}
