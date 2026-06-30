package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AccountSummaryResponse {

    private Long          id;
    private String        accountNumber;
    private BigDecimal    balance;
    private String        currency;
    private String        accountType;
    private String        status;
    private LocalDateTime createdAt;
}
