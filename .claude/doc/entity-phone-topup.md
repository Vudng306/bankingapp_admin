# Entity: PhoneTopup

- **Bảng**: `phone_topups`
- **Phần**: 3 (Bổ sung)
- **Mô tả**: Bản ghi chi tiết một lần nạp tiền điện thoại. Liên kết với một `Transaction` (phần trừ tiền).

## Schema

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Khóa chính |
| `transaction_id` | BIGINT | FK → transactions(id), NOT NULL | Giao dịch trừ tiền tương ứng |
| `carrier` | VARCHAR(20) | NOT NULL | Nhà mạng |
| `phone_number` | VARCHAR(15) | NOT NULL | Số điện thoại được nạp |
| `face_value` | DECIMAL(15,2) | NOT NULL | Mệnh giá nạp |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | Thời điểm |

### Giá trị `carrier` (gợi ý)
`viettel` | `mobifone` | `vinaphone` | `vietnamobile` | `itel`

## Ràng buộc & quy tắc
- Mỗi `phone_topups` gắn 1-1 với một `Transaction` type `topup`.
- `face_value` thuộc danh sách mệnh giá cố định (vd 10k, 20k, 50k, 100k, 200k, 500k).
- Số tiền trừ khỏi tài khoản = `face_value` (có thể cộng/trừ phí nếu mô phỏng).
- `phone_number` định dạng số điện thoại VN hợp lệ.
- Toàn bộ (trừ tiền + ghi transaction + ghi topup) trong cùng `@Transactional`.

## Quan hệ
- 1 PhoneTopup → 1 `Transaction`

## DDL
```sql
CREATE TABLE phone_topups (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id  BIGINT NOT NULL,
    carrier         VARCHAR(20) NOT NULL,
    phone_number    VARCHAR(15) NOT NULL,
    face_value      DECIMAL(15,2) NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    INDEX idx_topup_tx (transaction_id)
);
```
