# Entity: Card

- **Bảng**: `cards`
- **Phần**: 3 (Bổ sung)
- **Mô tả**: Thẻ ảo gắn với một tài khoản. Cho phép khóa/mở tạm thời và đặt hạn mức giao dịch.

## Schema

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Khóa chính |
| `account_id` | BIGINT | FK → accounts(id), NOT NULL | Tài khoản liên kết |
| `card_number` | VARCHAR(20) | UNIQUE, NOT NULL | Số thẻ (16 số) |
| `expiry_date` | DATE | NOT NULL | Ngày hết hạn |
| `cardholder_name` | VARCHAR(100) | NOT NULL | Tên in trên thẻ |
| `status` | ENUM('active','locked') | DEFAULT 'active' | Khóa/mở thẻ |
| `daily_limit` | DECIMAL(15,2) | NULL | Hạn mức giao dịch / ngày |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

## Ràng buộc & quy tắc
- `card_number` duy nhất, sinh tự động (16 số), hiển thị dạng che (vd `**** **** **** 1234`).
- `expiry_date` thường = ngày tạo + vài năm (vd 3 năm).
- `status = 'locked'`: thẻ tạm khóa, không dùng để giao dịch; user có thể mở lại.
- `daily_limit` NULL = không giới hạn; nếu có giá trị thì tổng giao dịch trong ngày qua thẻ không vượt quá.
- Đây là thẻ **ảo** (mô phỏng), không phát hành thật.
- ON DELETE CASCADE theo `account_id`.

## Quan hệ
- N Card → 1 `Account`

## DDL
```sql
CREATE TABLE cards (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT NOT NULL,
    card_number     VARCHAR(20) UNIQUE NOT NULL,
    expiry_date     DATE NOT NULL,
    cardholder_name VARCHAR(100) NOT NULL,
    status          ENUM('active','locked') DEFAULT 'active',
    daily_limit     DECIMAL(15,2),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    INDEX idx_cards_account (account_id)
);
```
