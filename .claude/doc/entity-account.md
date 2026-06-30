# Entity: Account

- **Bảng**: `accounts`
- **Phần**: 1 (Core)
- **Mô tả**: Tài khoản ngân hàng của người dùng. Một user có thể có nhiều tài khoản (thanh toán / tiết kiệm).

## Schema

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Khóa chính |
| `user_id` | BIGINT | FK → users(id), NOT NULL | Chủ tài khoản |
| `account_number` | VARCHAR(20) | UNIQUE, NOT NULL | Số tài khoản |
| `balance` | DECIMAL(15,2) | DEFAULT 0.00 | Số dư |
| `currency` | VARCHAR(3) | DEFAULT 'VND' | Loại tiền |
| `account_type` | ENUM('payment','savings') | DEFAULT 'payment' | Loại tài khoản |
| `status` | ENUM('active','locked') | DEFAULT 'active' | Trạng thái |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

## Ràng buộc & quy tắc
- `account_number` duy nhất toàn hệ thống.
- `balance` không bao giờ âm; mọi thay đổi phải qua `@Transactional`.
- `account_type = 'payment'` dùng cho giao dịch hàng ngày; `'savings'` liên kết với sổ tiết kiệm.
- `status = 'locked'` thì không cho phép giao dịch ra/vào.
- ON DELETE CASCADE theo `user_id`.

## Quan hệ
- N Account → 1 `User`
- 1 Account → N `Transaction` (vai trò from hoặc to)
- 1 Account → N `Savings` (nguồn trích tiền)

## DDL
```sql
CREATE TABLE accounts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    account_number  VARCHAR(20) UNIQUE NOT NULL,
    balance         DECIMAL(15,2) DEFAULT 0.00,
    currency        VARCHAR(3) DEFAULT 'VND',
    account_type    ENUM('payment', 'savings') DEFAULT 'payment',
    status          ENUM('active', 'locked') DEFAULT 'active',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_accounts_user (user_id)
);
```
