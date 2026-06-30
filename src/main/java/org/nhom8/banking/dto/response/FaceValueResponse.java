package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class FaceValueResponse {
    private BigDecimal amount;
    private String label;
}
