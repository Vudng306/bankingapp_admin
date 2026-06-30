package org.nhom8.banking.service;

import org.nhom8.banking.dto.response.DashboardSummaryResponse;
import org.nhom8.banking.dto.response.TransactionResponse;

import java.util.List;

public interface DashboardService {

    DashboardSummaryResponse getSummary(Long userId);

    List<TransactionResponse> getRecentTransactions(Long userId, int limit);
}
