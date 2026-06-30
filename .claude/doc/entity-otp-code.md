# Entity: OtpCode

- **Bảng**: `otp_codes`
- **Phần**: 1 (Core)
- **Mô tả**: Mã OTP dùng một lần cho đăng ký, đăng nhập, xác nhận giao dịch, đặt lại mật khẩu.

## Schema

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Khóa chính |
| `user_id` | BIGINT | FK → users(id), NOT NULL | Người dùng |
| `code` | VARCHAR(10) | NOT NULL | Mã OTP |
| `purpose` | ENUM(...) | NOT NULL | Mục đích sử dụng |
| `channel` | ENUM('sms','email') | NOT NULL | Kênh gửi |
| `expires_at` | DATETIME | NOT NULL | Thời điểm hết hạn |
| `is_used` | BOOLEAN | DEFAULT FALSE | Đã dùng chưa |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Thời điểm tạo |

### Giá trị `purpose`
`register` | `login` | `transfer` | `reset_password`

## Ràng buộc & quy tắc
- OTP hợp lệ khi `is_used = false` VÀ `expires_at > now()`.
- Sau khi xác thực thành công, đặt `is_used = true` (chống dùng lại).
- Thời hạn khuyến nghị: 5 phút kể từ `created_at`.
- Mỗi `purpose` nên dùng OTP mới nhất; OTP cũ coi như vô hiệu.
- ON DELETE CASCADE theo `user_id`.

## Quan hệ
- N OtpCode → 1 `User`

## DDL
```sql
CREATE TABLE otp_codes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    code        VARCHAR(10) NOT NULL,
    purpose     ENUM('register','login','transfer','reset_password') NOT NULL,
    channel     ENUM('sms','email') NOT NULL,
    expires_at  DATETIME NOT NULL,
    is_used     BOOLEAN DEFAULT FALSE,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_otp_user (user_id)
);
```
