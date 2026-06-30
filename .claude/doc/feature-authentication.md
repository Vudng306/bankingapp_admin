# Feature: Authentication

- **Phần**: 1 (Core)
- **Mô tả**: Đăng ký, đăng nhập, quên mật khẩu với xác thực OTP và mã PIN bảo mật.
- **Entity liên quan**: `User`, `OtpCode`

## Chức năng con
1. **Đăng ký**: nhập họ tên, email, phone, mật khẩu → sinh OTP (sms/email) → xác thực OTP → kích hoạt user + tạo tài khoản mặc định.
2. **Đăng nhập**: email/phone + mật khẩu → trả JWT. Tùy chọn OTP bước 2.
3. **Quên mật khẩu**: nhập email/phone → OTP `reset_password` → đặt mật khẩu mới.
4. **Thiết lập / đổi PIN**: PIN giao dịch riêng, hash bcrypt.

## Quy tắc nghiệp vụ
- Mật khẩu & PIN hash bằng bcrypt; không lưu plaintext.
- OTP hợp lệ: `is_used=false` và chưa hết hạn (5 phút). Dùng xong đặt `is_used=true`.
- JWT chứa `userId`, hết hạn cấu hình được (vd 24h).
- `status='locked'` không cho đăng nhập.
- Giai đoạn đầu có thể fake OTP (in ra log) để test, sau tích hợp email/SMS thật.

## Schema dữ liệu sử dụng
- Ghi/đọc `users` (password_hash, pin_hash, status).
- Ghi/đọc `otp_codes` (code, purpose, channel, expires_at, is_used).

## Gợi ý endpoint
- `POST /auth/register`
- `POST /auth/verify-otp`
- `POST /auth/login`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`
- `PUT  /auth/pin`
