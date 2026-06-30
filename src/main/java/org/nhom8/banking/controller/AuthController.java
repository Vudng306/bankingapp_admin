package org.nhom8.banking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.request.*;
import org.nhom8.banking.dto.response.AuthResponse;
import org.nhom8.banking.dto.response.OtpResponse;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Public ───────────────────────────────────────────────────────────────

    /**
     * Bước 1 đăng ký: gửi thông tin → nhận OTP.
     * Khi dev-mode=true, response có thêm trường devOtpCode để test.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OtpResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        OtpResponse data = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("OTP đã được gửi, vui lòng xác thực để hoàn tất đăng ký", data));
    }

    /**
     * Bước 2 đăng ký: xác thực OTP → nhận JWT (tự động đăng nhập).
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        AuthResponse data = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng ký thành công", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", data));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<OtpResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        OtpResponse data = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("OTP đặt lại mật khẩu đã được gửi", data));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Mật khẩu đã được đặt lại thành công"));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        OtpResponse data = authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("OTP mới đã được gửi", data));
    }

    // ── Protected (yêu cầu JWT) ───────────────────────────────────────────────

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công"));
    }

    @PutMapping("/pin")
    public ResponseEntity<ApiResponse<Void>> setPin(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody SetPinRequest request) {

        authService.setPin(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Thiết lập PIN thành công"));
    }

    @PutMapping("/pin/change")
    public ResponseEntity<ApiResponse<Void>> changePin(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ChangePinRequest request) {

        authService.changePin(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đổi PIN thành công"));
    }
}
