package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountLookupResponse {
    private String accountNumber;
    private String accountName;
    private String bankName;
}
