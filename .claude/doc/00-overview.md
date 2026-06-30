# Banking App — Tổng quan dự án

## Bối cảnh
Ứng dụng ngân hàng di động (đồ án sinh viên). Backend: **Java Spring Boot**, Database: **MySQL**.
Tài liệu này được viết để con người và LLM cùng đọc hiểu kiến trúc dữ liệu và nghiệp vụ.

## Stack kỹ thuật
- **Backend**: Spring Boot (Spring Web, Spring Data JPA, Spring Security, Validation)
- **Auth**: JWT + bcrypt (password & PIN)
- **Database**: MySQL 8.x
- **Kiểu tiền tệ**: `DECIMAL(15,2)`, mặc định VND

## Phạm vi tài liệu
Tài liệu này bao phủ **Phần 1 (Core)** và **Phần 2 (Nâng cao)**.

### Phần 1 — Core
Đăng ký/đăng nhập/quên mật khẩu (OTP, PIN), Dashboard, Chuyển tiền (nội bộ + liên ngân hàng giả lập),
Lịch sử giao dịch (lọc/tìm kiếm/xuất file), Quản lý tài khoản cá nhân.

### Phần 2 — Nâng cao
QR chuyển tiền, Tiết kiệm (kỳ hạn + lãi suất + tính lãi tự động),
Push Notification, Báo cáo chi tiêu.

### Phần 3 — Bổ sung
Nạp tiền điện thoại, Quản lý thẻ (thẻ ảo, khóa/mở, hạn mức),
Quản lý thiết bị đăng nhập (đăng xuất từ xa).

## Cấu trúc tài liệu
```
00-overview.md                       <- file này

# Entities (schema + ràng buộc)
entity-user.md
entity-account.md
entity-transaction.md
entity-otp-code.md
entity-notification.md
entity-savings.md
entity-device.md
entity-card.md
entity-phone-topup.md

# Features (nghiệp vụ)
feature-authentication.md
feature-dashboard.md
feature-transfer.md
feature-transaction-history.md
feature-account-management.md
feature-qr-transfer.md
feature-savings.md
feature-push-notification.md
feature-spending-report.md
feature-phone-topup.md
feature-card-management.md
feature-device-management.md
```

## Danh sách entity (Phần 1 + 2 + 3)
| Entity | Bảng | Phần | Vai trò |
|--------|------|------|---------|
| User | `users` | 1 | Người dùng |
| Account | `accounts` | 1 | Tài khoản ngân hàng |
| Transaction | `transactions` | 1 | Giao dịch |
| OtpCode | `otp_codes` | 1 | Mã OTP |
| Notification | `notifications` | 1 | Thông báo |
| Savings | `savings` | 2 | Sổ tiết kiệm |
| Device | `devices` | 2 | Thiết bị / push token |
| Card | `cards` | 3 | Thẻ ảo |
| PhoneTopup | `phone_topups` | 3 | Nạp tiền điện thoại |

## Quan hệ tổng quát
- Một `User` có nhiều `Account`.
- Một `Account` tham gia nhiều `Transaction` (vai trò gửi hoặc nhận).
- Một `User` có nhiều `OtpCode`, `Notification`, `Savings`, `Device`.
- Một `Savings` trích tiền từ một `Account` (source_account).
- Một `Account` có nhiều `Card`.
- Một `PhoneTopup` gắn 1-1 với một `Transaction`.

## Quy ước chung
- Mọi bảng có khóa chính `id BIGINT AUTO_INCREMENT`.
- Mật khẩu và PIN **luôn hash** (bcrypt), không lưu plaintext.
- Mọi thao tác thay đổi số dư phải nằm trong **DB transaction** (`@Transactional`).
- Timestamp dùng `DATETIME`; `created_at` mặc định `CURRENT_TIMESTAMP`.
