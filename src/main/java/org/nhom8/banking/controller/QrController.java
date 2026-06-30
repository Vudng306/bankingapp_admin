package org.nhom8.banking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.request.QrGenerateRequest;
import org.nhom8.banking.dto.response.QrResponse;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.QrService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/qr")
@RequiredArgsConstructor
@Validated
public class QrController {

    private final QrService qrService;

    /**
     * Sinh nội dung QR theo chuẩn VietQR cho tài khoản của user hiện tại.
     *
     * GET /qr/generate?accountId=1
     * GET /qr/generate?accountId=1&amount=100000&description=Thanh+toan
     *
     * Response trả về chuỗi qrContent — app Android dùng để vẽ ảnh QR (ZXing, v.v.).
     */
    @GetMapping("/generate")
    public ResponseEntity<ApiResponse<QrResponse>> generate(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @ModelAttribute QrGenerateRequest request) {

        QrResponse data = qrService.generate(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Tạo mã QR thành công", data));
    }
}
