package org.nhom8.banking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.request.ConfirmTransferRequest;
import org.nhom8.banking.dto.request.InternalTransferRequest;
import org.nhom8.banking.dto.request.InterbankTransferRequest;
import org.nhom8.banking.dto.response.AccountLookupResponse;
import org.nhom8.banking.dto.response.TransactionResponse;
import org.nhom8.banking.dto.response.TransferInitiateResponse;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /** Tra cứu tài khoản nội bộ trước khi chuyển tiền — trả về tên chủ tài khoản. */
    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<AccountLookupResponse>> lookup(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String accountNumber) {
        return ResponseEntity.ok(ApiResponse.ok(transferService.lookupAccount(accountNumber)));
    }

    /**
     * Bước 1 — Chuyển khoản nội bộ.
     * Xác thực PIN + validate → lưu phiên → gửi OTP xác nhận.
     */
    @PostMapping("/internal")
    public ResponseEntity<ApiResponse<TransferInitiateResponse>> initiateInternal(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody InternalTransferRequest request) {

        TransferInitiateResponse data = transferService.initiateInternal(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(
                "OTP xác nhận giao dịch đã được gửi", data));
    }

    /**
     * Bước 1 — Chuyển khoản liên ngân hàng.
     * Xác thực PIN + validate → lưu phiên → gửi OTP xác nhận.
     */
    @PostMapping("/interbank")
    public ResponseEntity<ApiResponse<TransferInitiateResponse>> initiateInterbank(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody InterbankTransferRequest request) {

        TransferInitiateResponse data = transferService.initiateInterbank(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(
                "OTP xác nhận giao dịch đã được gửi", data));
    }

    /**
     * Bước 2 — Xác nhận giao dịch bằng OTP.
     * Dùng chung cho cả nội bộ và liên ngân hàng.
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<TransactionResponse>> confirmTransfer(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ConfirmTransferRequest request) {

        TransactionResponse data = transferService.confirmTransfer(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Giao dịch đã được thực thi", data));
    }
}
