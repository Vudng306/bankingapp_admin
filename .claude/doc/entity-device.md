# Entity: Device

- **Bảng**: `devices`
- **Phần**: 2 (Nâng cao) — mở rộng cho Phần 3 (biometric, quản lý thiết bị)
- **Mô tả**: Thiết bị đã đăng nhập của user. Lưu push token cho Firebase Cloud Messaging; cờ biometric cho đăng nhập sinh trắc học.

## Schema

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Khóa chính |
| `user_id` | BIGINT | FK → users(id), NOT NULL | Chủ thiết bị |
| `device_name` | VARCHAR(100) | NULL | Tên thiết bị (vd "Samsung A52") |
| `device_id` | VARCHAR(255) | NULL | Định danh thiết bị |
| `push_token` | VARCHAR(255) | NULL | FCM token để gửi push |
| `biometric_enabled` | BOOLEAN | DEFAULT FALSE | Bật vân tay / Face ID |
| `last_login_at` | DATETIME | NULL | Lần đăng nhập gần nhất |
| `is_active` | BOOLEAN | DEFAULT TRUE | FALSE = đã đăng xuất từ xa |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

## Ràng buộc & quy tắc
- `push_token` cập nhật mỗi lần FCM cấp token mới cho thiết bị.
- Gửi push notification: lấy `push_token` của các device `is_active = true`.
- `biometric_enabled` chỉ là cờ; **không lưu dữ liệu sinh trắc học** (giữ trong secure hardware của thiết bị).
- Đăng xuất từ xa: đặt `is_active = false` (Phần 3).
- ON DELETE CASCADE theo `user_id`.

## Quan hệ
- N Device → 1 `User`

## DDL
```sql
CREATE TABLE devices (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    device_name         VARCHAR(100),
    device_id           VARCHAR(255),
    push_token          VARCHAR(255),
    biometric_enabled   BOOLEAN DEFAULT FALSE,
    last_login_at       DATETIME,
    is_active           BOOLEAN DEFAULT TRUE,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_devices_user (user_id)
);
```
