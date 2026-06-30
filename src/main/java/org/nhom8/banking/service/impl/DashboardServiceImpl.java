package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import org.nhom8.banking.dto.response.AccountSummaryResponse;
import org.nhom8.banking.dto.response.DashboardSummaryResponse;
import org.nhom8.banking.dto.response.TransactionResponse;
import org.nhom8.banking.entity.Account;
import org.nhom8.banking.entity.Transaction;
import org.nhom8.banking.repository.AccountRepository;
import org.nhom8.banking.repository.NotificationRepository;
import org.nhom8.banking.repository.TransactionRepository;
import org.nhom8.banking.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AccountRepository      accountRepository;
    private final TransactionRepository  transactionRepository;
    private final NotificationRepository notificationRepository;

    private static final Sort BY_CREATED_AT_DESC = Sort.by(Sort.Direction.DESC, "createdAt");

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);

        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Long> accountIds = accounts.stream().map(Account::getId).toList();
        Set<Long>  accountIdSet = Set.copyOf(accountIds);

        List<TransactionResponse> recentTxs = accountIds.isEmpty() ? List.of()
                : transactionRepository
                        .findDistinctByFromAccount_IdInOrToAccount_IdIn(
                                accountIds, accountIds,
                                PageRequest.of(0, 5, BY_CREATED_AT_DESC))
                        .stream()
                        .map(tx -> toResponse(tx, accountIdSet))
                        .toList();

        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);

        return DashboardSummaryResponse.builder()
                .accounts(accounts.stream().map(this::toAccountSummary).toList())
                .totalBalance(totalBalance)
                .recentTransactions(recentTxs)
                .unreadNotificationCount(unreadCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactions(Long userId, int limit) {
        List<Long> accountIds = accountRepository.findByUserId(userId)
                .stream().map(Account::getId).toList();

        if (accountIds.isEmpty()) return List.of();

        Set<Long> accountIdSet = Set.copyOf(accountIds);
        return transactionRepository
                .findDistinctByFromAccount_IdInOrToAccount_IdIn(
                        accountIds, accountIds,
                        PageRequest.of(0, Math.min(limit, 50), BY_CREATED_AT_DESC))
                .stream()
                .map(tx -> toResponse(tx, accountIdSet))
                .toList();
    }

    // -------------------------------------------------------------------------

    private TransactionResponse toResponse(Transaction tx, Set<Long> myAccountIds) {
        boolean isDebit = tx.getFromAccount() != null
                && myAccountIds.contains(tx.getFromAccount().getId());

        String counterpartAccount;
        String counterpartBank = null;

        if (isDebit) {
            if (tx.getToAccount() != null) {
                counterpartAccount = tx.getToAccount().getAccountNumber();
            } else {
                counterpartAccount = tx.getToExternalAccount();
                counterpartBank    = tx.getToBankCode();
            }
        } else {
            counterpartAccount = tx.getFromAccount() != null
                    ? tx.getFromAccount().getAccountNumber()
                    : null;
        }

        return TransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType().name())
                .status(tx.getStatus().name())
                .amount(tx.getAmount())
                .fee(tx.getFee())
                .direction(isDebit ? "DEBIT" : "CREDIT")
                .counterpartAccount(counterpartAccount)
                .counterpartBank(counterpartBank)
                .description(tx.getDescription())
                .referenceCode(tx.getReferenceCode())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private AccountSummaryResponse toAccountSummary(Account a) {
        return AccountSummaryResponse.builder()
                .id(a.getId())
                .accountNumber(a.getAccountNumber())
                .balance(a.getBalance())
                .currency(a.getCurrency())
                .accountType(a.getAccountType().name())
                .status(a.getStatus().name())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
