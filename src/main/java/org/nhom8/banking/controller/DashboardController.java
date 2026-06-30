package org.nhom8.banking.controller;

import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.response.DashboardSummaryResponse;
import org.nhom8.banking.dto.response.TransactionResponse;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Tóm tắt Dashboard: danh sách tài khoản + tổng số dư + giao dịch gần đây + badge thông báo
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getSummary(user.getId())));
    }

    /**
     * Giao dịch gần đây (có thể gọi riêng, mặc định 10 bản ghi)
     */
    @GetMapping("/transactions/recent")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getRecentTransactions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getRecentTransactions(user.getId(), limit)));
    }
}
