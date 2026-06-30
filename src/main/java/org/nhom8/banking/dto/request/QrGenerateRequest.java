package org.nhom8.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QrGenerateRequest {

    @NotNull(message = "accountId là bắt buộc")
    private Long accountId;

    @DecimalMin(value = "1000", message = "Số tiền tối thiểu là 1,000 VND")
    private BigDecimal amount;

    @Size(max = 25, message = "Nội dung tối đa 25 ký tự")
    private String description;
}
