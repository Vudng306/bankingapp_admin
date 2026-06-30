# Feature: Savings (Tiết kiệm)

- **Phần**: 2 (Nâng cao)
- **Mô tả**: Mở sổ tiết kiệm có kỳ hạn, lãi suất, tính lãi tự động.
- **Entity liên quan**: `Savings`, `Account`, `Transaction`

## Chức năng con
1. **Mở sổ**: chọn tài khoản nguồn, nhập gốc, chọn kỳ hạn → trích tiền → tạo sổ.
2. **Xem danh sách sổ**: gốc, lãi suất, lãi đã cộng dồn, ngày đáo hạn, trạng thái.
3. **Tính lãi tự động**: scheduled task cập nhật `accrued_interest`.
4. **Rút sổ**: hoàn gốc + lãi về tài khoản.

## Quy tắc nghiệp vụ
- Mở sổ: kiểm tra số dư nguồn đủ → trừ `principal` → ghi `Transaction` `savings_deposit` → tạo `Savings` (`status=active`). Tất cả trong cùng `@Transactional`.
- `maturity_date = start_date + term_months`.
- Tính lãi đơn giản: `principal * interest_rate/100 * (số ngày/365)`.
- Scheduled task (`@Scheduled`) chạy định kỳ (vd hàng ngày) cập nhật `accrued_interest`, chuyển `status` sang `matured` khi tới hạn.
- Rút sổ: cộng `principal + accrued_interest` về account → ghi `Transaction` `savings_withdraw` → `status=withdrawn`. Trong cùng `@Transactional`.

## Schema dữ liệu sử dụng
- Ghi/đọc `savings`.
- Cập nhật `accounts.balance`.
- Ghi `transactions` (`savings_deposit`, `savings_withdraw`).

## Gợi ý endpoint
- `POST /savings`            (mở sổ)
- `GET  /savings`            (danh sách sổ)
- `POST /savings/{id}/withdraw`
