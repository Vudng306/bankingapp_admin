package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawSavingsRequest {

    @NotBlank(message = "pin là bắt buộc")
    private String pin;
}
