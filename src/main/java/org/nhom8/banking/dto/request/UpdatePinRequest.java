package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePinRequest {

    /**
     * PIN hiện tại — bắt buộc khi user đã có PIN trước đó.
     * Null / bỏ qua khi thiết lập PIN lần đầu.
     */
    private String currentPin;

    @NotBlank(message = "PIN mới không được để trống")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN phải là 6 chữ số")
    private String newPin;
}
