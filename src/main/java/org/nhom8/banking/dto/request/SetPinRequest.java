package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SetPinRequest {

    @NotBlank(message = "Mã PIN không được để trống")
    @Pattern(regexp = "^[0-9]{6}$", message = "Mã PIN phải là 6 chữ số")
    private String pin;
}
