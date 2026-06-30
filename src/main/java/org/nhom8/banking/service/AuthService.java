package org.nhom8.banking.service;

import org.nhom8.banking.dto.request.*;
import org.nhom8.banking.dto.response.AuthResponse;
import org.nhom8.banking.dto.response.OtpResponse;

public interface AuthService {

    /** Đăng ký → lưu user (LOCKED) → sinh & gửi OTP REGISTER */
    OtpResponse register(RegisterRequest request);

    /** Xác thực OTP đăng ký → ACTIVE user + tạo account → trả JWT */
    AuthResponse verifyOtp(VerifyOtpRequest request);

    /** Đăng nhập email/phone + password → trả JWT */
    AuthResponse login(LoginRequest request);

    /** Quên mật khẩu → sinh & gửi OTP RESET_PASSWORD */
    OtpResponse forgotPassword(ForgotPasswordRequest request);

    /** Đặt lại mật khẩu: xác thực OTP RESET_PASSWORD + lưu password mới */
    void resetPassword(ResetPasswordRequest request);

    /** Gửi lại OTP (throttle cần thêm sau) */
    OtpResponse resendOtp(ResendOtpRequest request);

    /** Đổi mật khẩu (đã đăng nhập) */
    void changePassword(Long userId, ChangePasswordRequest request);

    /** Thiết lập hoặc ghi đè PIN (đã đăng nhập) */
    void setPin(Long userId, SetPinRequest request);

    /** Đổi PIN: xác minh PIN cũ → lưu PIN mới */
    void changePin(Long userId, ChangePinRequest request);
}
