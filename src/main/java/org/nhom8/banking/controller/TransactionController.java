package org.nhom8.banking.controller;

import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.request.TransactionFilterRequest;
import org.nhom8.banking.dto.response.TransactionReceiptResponse;
import org.nhom8.banking.dto.response.TransactionResponse;
import org.nhom8.banking.entity.Transaction;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    private static final Sort BY_CREATED_AT_DESC = Sort.by(Sort.Direction.DESC, "createdAt");

    /** Biên lai chi tiết của một giao dịch */
    @GetMapping("/{id}/receipt")
    public ResponseEntity<ApiResponse<TransactionReceiptResponse>> getReceipt(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.ok(
                transactionService.getReceipt(user.getId(), id)));
    }

    /**
     * Lịch sử giao dịch — phân trang + lọc động.
     * Mọi param ngoài accountId đều optional.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getHistory(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long accountId,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) Transaction.TransactionStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setAccountId(accountId);
        filter.setType(type);
        filter.setStatus(status);
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setKeyword(keyword);

        Page<TransactionResponse> data = transactionService.getHistory(
                user.getId(), filter,
                PageRequest.of(page, Math.min(size, 50), BY_CREATED_AT_DESC));

        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
