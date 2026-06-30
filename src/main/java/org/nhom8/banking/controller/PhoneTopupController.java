package org.nhom8.banking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.request.ConfirmTransferRequest;
import org.nhom8.banking.dto.request.TopupInitiateRequest;
import org.nhom8.banking.dto.response.*;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.PhoneTopupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/topup")
@RequiredArgsConstructor
public class PhoneTopupController {

    private final PhoneTopupService phoneTopupService;

    /**
     * Danh sách nhà mạng được hỗ trợ.
     * GET /topup/carriers
     */
    @GetMapping("/carriers")
    public ResponseEntity<ApiResponse<List<CarrierResponse>>> getCarriers() {
        return ResponseEntity.ok(
                ApiResponse.ok("Danh sách nhà mạng", phoneTopupService.getCarriers())
        );
    }

    /**
     * Danh sách mệnh giá nạp tiền.
     * GET /topup/face-values
     */
    @GetMapping("/face-values")
    public ResponseEntity<ApiResponse<List<FaceValueResponse>>> getFaceValues() {
        return ResponseEntity.ok(
                ApiResponse.ok("Danh sách mệnh giá", phoneTopupService.getFaceValues())
        );
    }

    /**
     * Bước 1 — Xác thực PIN, khởi tạo phiên nạp, gửi OTP.
     * POST /topup/initiate
     * Body: { fromAccountId, phoneNumber, carrier, faceValue, pin }
     */
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<TopupInitiateResponse>> initiate(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody TopupInitiateRequest request) {

        TopupInitiateResponse data = phoneTopupService.initiateTopup(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Vui lòng nhập mã OTP để xác nhận", data));
    }

    /**
     * Bước 2 — Xác thực OTP, thực thi nạp tiền.
     * POST /topup/confirm
     * Body: { confirmToken, otpCode }
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<TopupReceiptResponse>> confirm(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ConfirmTransferRequest request) {

        TopupReceiptResponse data = phoneTopupService.confirmTopup(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Nạp tiền thành công", data));
    }
}
