package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DashboardSummaryResponse {

    private List<AccountSummaryResponse> accounts;
    private BigDecimal                   totalBalance;
    private List<TransactionResponse>    recentTransactions;
    private long                         unreadNotificationCount;
}
