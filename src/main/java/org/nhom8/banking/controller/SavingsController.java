package org.nhom8.banking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.request.OpenSavingsRequest;
import org.nhom8.banking.dto.request.WithdrawSavingsRequest;
import org.nhom8.banking.dto.response.SavingsResponse;
import org.nhom8.banking.dto.response.WithdrawSavingsResponse;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.SavingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/savings")
@RequiredArgsConstructor
public class SavingsController {

    private final SavingsService savingsService;

    /**
     * Mở sổ tiết kiệm.
     * Yêu cầu PIN; trích tiền từ fromAccount trong cùng một DB transaction.
     *
     * POST /savings
     * Body: { fromAccountId, amount, termMonths, pin }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SavingsResponse>> open(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody OpenSavingsRequest request) {

        SavingsResponse data = savingsService.open(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Mở sổ tiết kiệm thành công", data));
    }

    /**
     * Danh sách tất cả sổ tiết kiệm của user hiện tại.
     *
     * GET /savings
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SavingsResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails user) {

        List<SavingsResponse> data = savingsService.list(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Danh sách sổ tiết kiệm", data));
    }

    /**
     * Chi tiết một sổ tiết kiệm.
     *
     * GET /savings/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsResponse>> detail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {

        SavingsResponse data = savingsService.detail(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * Tất toán sổ tiết kiệm.
     * - Đúng hạn / quá hạn → nhận gốc + lãi đủ kỳ hạn.
     * - Rút sớm           → nhận gốc + lãi không kỳ hạn × số ngày thực gửi.
     *
     * POST /savings/{id}/withdraw
     * Body: { pin }
     */
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<WithdrawSavingsResponse>> withdraw(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody WithdrawSavingsRequest request) {

        WithdrawSavingsResponse data = savingsService.withdraw(user.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Tất toán thành công", data));
    }
}
