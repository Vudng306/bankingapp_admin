# Entity: Savings

- **Bảng**: `savings`
- **Phần**: 2 (Nâng cao)
- **Mô tả**: Sổ tiết kiệm có kỳ hạn. Trích tiền gốc từ một tài khoản nguồn, tính lãi tự động theo kỳ hạn.

## Schema

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Khóa chính |
| `user_id` | BIGINT | FK → users(id), NOT NULL | Chủ sổ |
| `source_account_id` | BIGINT | FK → accounts(id), NOT NULL | Tài khoản trích tiền gốc |
| `principal` | DECIMAL(15,2) | NOT NULL | Tiền gốc |
| `interest_rate` | DECIMAL(5,2) | NOT NULL | Lãi suất %/năm (vd 5.50) |
| `term_months` | INT | NOT NULL | Kỳ hạn (tháng): 3, 6, 12... |
| `start_date` | DATE | NOT NULL | Ngày mở sổ |
| `maturity_date` | DATE | NOT NULL | Ngày đáo hạn |
| `accrued_interest` | DECIMAL(15,2) | DEFAULT 0.00 | Lãi đã cộng dồn |
| `status` | ENUM('active','matured','withdrawn') | DEFAULT 'active' | Trạng thái |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

## Ràng buộc & quy tắc
- `principal > 0` và `<=` số dư tài khoản nguồn tại thời điểm mở.
- `maturity_date = start_date + term_months`.
- Mở sổ: trừ `principal` khỏi `source_account`, ghi `Transaction` type `savings_deposit`, trong cùng `@Transactional`.
- Tính lãi: công thức đơn giản `principal * interest_rate/100 * (số ngày / 365)`; cập nhật `accrued_interest` định kỳ qua scheduled task.
- `status = 'matured'` khi `now() >= maturity_date`.
- Rút sổ: cộng `principal + accrued_interest` về tài khoản, ghi `Transaction` type `savings_withdraw`, đặt `status = 'withdrawn'`.

## Quan hệ
- N Savings → 1 `User`
- N Savings → 1 `Account` (source)

## DDL
```sql
CREATE TABLE savings (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    source_account_id   BIGINT NOT NULL,
    principal           DECIMAL(15,2) NOT NULL,
    interest_rate       DECIMAL(5,2) NOT NULL,
    term_months         INT NOT NULL,
    start_date          DATE NOT NULL,
    maturity_date       DATE NOT NULL,
    accrued_interest    DECIMAL(15,2) DEFAULT 0.00,
    status              ENUM('active','matured','withdrawn') DEFAULT 'active',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    INDEX idx_savings_user (user_id)
);
```
