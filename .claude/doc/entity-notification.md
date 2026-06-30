# Entity: Notification

- **Bảng**: `notifications`
- **Phần**: 1 (Core) — tái sử dụng cho Push Notification ở Phần 2
- **Mô tả**: Thông báo trong ứng dụng (in-app). Cũng là nguồn nội dung cho push notification.

## Schema

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Khóa chính |
| `user_id` | BIGINT | FK → users(id), NOT NULL | Người nhận |
| `title` | VARCHAR(150) | NOT NULL | Tiêu đề |
| `content` | TEXT | NULL | Nội dung |
| `type` | ENUM('transaction','balance','system') | DEFAULT 'system' | Loại thông báo |
| `is_read` | BOOLEAN | DEFAULT FALSE | Đã đọc chưa |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Thời điểm |

## Ràng buộc & quy tắc
- `type = 'transaction'`: sinh sau mỗi giao dịch.
- `type = 'balance'`: dùng cho biến động số dư (gắn với push ở Phần 2).
- `type = 'system'`: thông báo hệ thống chung.
- Đếm `is_read = false` để hiển thị badge số thông báo chưa đọc trên Dashboard.
- ON DELETE CASCADE theo `user_id`.

## Quan hệ
- N Notification → 1 `User`

## DDL
```sql
CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    title       VARCHAR(150) NOT NULL,
    content     TEXT,
    type        ENUM('transaction','balance','system') DEFAULT 'system',
    is_read     BOOLEAN DEFAULT FALSE,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user (user_id)
);
```
