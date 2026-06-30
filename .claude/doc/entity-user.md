# Entity: User

- **Bảng**: `users`
- **Phần**: 1 (Core)
- **Mô tả**: Người dùng của ứng dụng. Lưu thông tin định danh và thông tin xác thực.

## Schema

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Khóa chính |
| `full_name` | VARCHAR(100) | NOT NULL | Họ tên |
| `email` | VARCHAR(150) | UNIQUE, NOT NULL | Email đăng nhập |
| `phone` | VARCHAR(15) | UNIQUE, NOT NULL | Số điện thoại |
| `password_hash` | VARCHAR(255) | NOT NULL | Mật khẩu đã hash (bcrypt) |
| `pin_hash` | VARCHAR(255) | NULL | Mã PIN giao dịch đã hash (bcrypt) |
| `avatar_url` | VARCHAR(255) | NULL | Đường dẫn ảnh đại diện |
| `status` | ENUM('active','locked') | DEFAULT 'active' | Trạng thái tài khoản |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | Ngày cập nhật |

## Ràng buộc & quy tắc
- `email` và `phone` là duy nhất toàn hệ thống.
- `password_hash` và `pin_hash` không bao giờ lưu plaintext; hash bằng bcrypt.
- `pin_hash` có thể NULL khi user mới đăng ký chưa thiết lập PIN.
- `status = 'locked'` thì user không được đăng nhập / giao dịch.

## Quan hệ
- 1 User → N `Account`
- 1 User → N `OtpCode`, `Notification`, `Savings`, `Device`

## DDL
```sql
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(150) UNIQUE NOT NULL,
    phone           VARCHAR(15) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    pin_hash        VARCHAR(255),
    avatar_url      VARCHAR(255),
    status          ENUM('active', 'locked') DEFAULT 'active',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```
