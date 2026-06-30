package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.BankCode;
import org.nhom8.banking.dto.request.TransactionFilterRequest;
import org.nhom8.banking.dto.response.TransactionReceiptResponse;
import org.nhom8.banking.dto.response.TransactionResponse;
import org.nhom8.banking.entity.Account;
import org.nhom8.banking.entity.Transaction;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.AccountRepository;
import org.nhom8.banking.repository.TransactionRepository;
import org.nhom8.banking.repository.TransactionSpec;
import org.nhom8.banking.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository     accountRepository;

    // ── Biên lai ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TransactionReceiptResponse getReceipt(Long userId, Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        boolean isSender   = tx.getFromAccount() != null
                && tx.getFromAccount().getUser().getId().equals(userId);
        boolean isReceiver = tx.getToAccount() != null
                && tx.getToAccount().getUser().getId().equals(userId);

        if (!isSender && !isReceiver) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        return buildReceipt(tx);
    }

    // ── Lịch sử giao dịch ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getHistory(Long userId, TransactionFilterRequest filter, Pageable pageable) {
        Long accountId = filter.getAccountId();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        return transactionRepository
                .findAll(TransactionSpec.build(filter), pageable)
                .map(tx -> toResponse(tx, accountId));
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private TransactionReceiptResponse buildReceipt(Transaction tx) {
        boolean isInterbank = tx.getType() == Transaction.TransactionType.INTERBANK;

        String fromNumber = tx.getFromAccount() != null
                ? tx.getFromAccount().getAccountNumber() : null;
        String fromName = tx.getFromAccount() != null
                ? tx.getFromAccount().getUser().getFullName() : null;

        String toNumber, toName, toBankCode, toBankName;
        if (isInterbank) {
            toNumber   = tx.getToExternalAccount();
            toName     = tx.getToExternalAccountName();
            toBankCode = tx.getToBankCode();
            toBankName = resolveBankName(tx.getToBankCode());
        } else {
            toNumber   = tx.getToAccount() != null ? tx.getToAccount().getAccountNumber() : null;
            toName     = tx.getToAccount() != null ? tx.getToAccount().getUser().getFullName() : null;
            toBankCode = null;
            toBankName = null;
        }

        BigDecimal fee   = tx.getFee() != null ? tx.getFee() : BigDecimal.ZERO;
        BigDecimal total = tx.getAmount().add(fee);

        return TransactionReceiptResponse.builder()
                .id(tx.getId())
                .referenceCode(tx.getReferenceCode())
                .type(tx.getType().name())
                .status(tx.getStatus().name())
                .fromAccountNumber(fromNumber)
                .fromAccountName(fromName)
                .toAccountNumber(toNumber)
                .toAccountName(toName)
                .toBankCode(toBankCode)
                .toBankName(toBankName)
                .amount(tx.getAmount())
                .fee(fee)
                .totalDeducted(total)
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private TransactionResponse toResponse(Transaction tx, Long viewerAccountId) {
        boolean isDebit     = tx.getFromAccount() != null
                && tx.getFromAccount().getId().equals(viewerAccountId);
        boolean isInterbank = tx.getType() == Transaction.TransactionType.INTERBANK;

        String counterpartAccount;
        String counterpartName;

        if (isDebit) {
            if (isInterbank) {
                counterpartAccount = tx.getToExternalAccount();
                counterpartName    = tx.getToExternalAccountName();
            } else {
                counterpartAccount = tx.getToAccount() != null ? tx.getToAccount().getAccountNumber() : null;
                counterpartName    = tx.getToAccount() != null ? tx.getToAccount().getUser().getFullName() : null;
            }
        } else {
            counterpartAccount = tx.getFromAccount() != null ? tx.getFromAccount().getAccountNumber() : null;
            counterpartName    = tx.getFromAccount() != null ? tx.getFromAccount().getUser().getFullName() : null;
        }

        return TransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType().name())
                .status(tx.getStatus().name())
                .amount(tx.getAmount())
                .fee(tx.getFee())
                .direction(isDebit ? "DEBIT" : "CREDIT")
                .counterpartAccount(counterpartAccount)
                .counterpartName(counterpartName)
                .counterpartBank(isInterbank ? tx.getToBankCode() : null)
                .description(tx.getDescription())
                .referenceCode(tx.getReferenceCode())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private String resolveBankName(String code) {
        if (code == null) return null;
        try {
            return BankCode.fromCode(code).getDisplayName();
        } catch (Exception e) {
            return code;
        }
    }
}
