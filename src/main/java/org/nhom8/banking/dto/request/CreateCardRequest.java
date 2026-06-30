package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateCardRequest {

    @NotNull(message = "Vui lòng chọn tài khoản liên kết")
    private Long accountId;
}
