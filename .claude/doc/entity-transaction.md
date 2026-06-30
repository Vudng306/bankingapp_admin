# Entity: Transaction

- **Bảng**: `transactions`
- **Phần**: 1 (Core)
- **Mô tả**: Bản ghi giao dịch. Bao phủ chuyển nội bộ, liên ngân hàng giả lập, nạp tiền điện thoại, gửi/rút tiết kiệm.

## Schema

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Khóa chính |
| `from_account_id` | BIGINT | FK → accounts(id), NULL | Tài khoản nguồn (NULL nếu nạp từ ngoài) |
| `to_account_id` | BIGINT | FK → accounts(id), NULL | Tài khoản đích nội bộ (NULL nếu chuyển ra ngoài) |
| `to_external_account` | VARCHAR(20) | NULL | Số tài khoản ngoài (liên ngân hàng) |
| `to_bank_code` | VARCHAR(20) | NULL | Mã ngân hàng nhận |
| `amount` | DECIMAL(15,2) | NOT NULL | Số tiền |
| `fee` | DECIMAL(15,2) | DEFAULT 0.00 | Phí giao dịch |
| `type` | ENUM(...) | NOT NULL | Loại giao dịch |
| `status` | ENUM('pending','success','failed') | DEFAULT 'pending' | Trạng thái |
| `description` | VARCHAR(255) | NULL | Nội dung chuyển khoản |
| `reference_code` | VARCHAR(30) | UNIQUE, NOT NULL | Mã biên lai |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Thời điểm |

### Giá trị `type`
`internal` | `interbank` | `topup` | `savings_deposit` | `savings_withdraw`

## Ràng buộc & quy tắc
- Chuyển **nội bộ**: `from_account_id` + `to_account_id` đều có giá trị; `to_external_account`/`to_bank_code` = NULL.
- Chuyển **liên ngân hàng**: `to_account_id` = NULL; dùng `to_external_account` + `to_bank_code`.
- `amount > 0`.
- `reference_code` duy nhất, sinh tự động cho mỗi giao dịch.
- Giao dịch ghi nhận trong cùng `@Transactional` với việc cập nhật số dư.
- `status = 'failed'` không được làm thay đổi số dư.

## Quan hệ
- N Transaction → 1 `Account` (from)
- N Transaction → 1 `Account` (to)

## DDL
```sql
CREATE TABLE transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_account_id     BIGINT,
    to_account_id       BIGINT,
    to_external_account VARCHAR(20),
    to_bank_code        VARCHAR(20),
    amount              DECIMAL(15,2) NOT NULL,
    fee                 DECIMAL(15,2) DEFAULT 0.00,
    type                ENUM('internal','interbank','topup','savings_deposit','savings_withdraw') NOT NULL,
    status              ENUM('pending','success','failed') DEFAULT 'pending',
    description         VARCHAR(255),
    reference_code      VARCHAR(30) UNIQUE NOT NULL,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_account_id) REFERENCES accounts(id),
    FOREIGN KEY (to_account_id) REFERENCES accounts(id),
    INDEX idx_tx_from (from_account_id),
    INDEX idx_tx_to (to_account_id),
    INDEX idx_tx_created (created_at),
    INDEX idx_tx_type (type)
);
```
