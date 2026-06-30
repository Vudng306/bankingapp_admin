# Feature: Transfer (Chuyển tiền)

- **Phần**: 1 (Core) — chức năng lõi, nhạy cảm nhất
- **Mô tả**: Chuyển tiền nội bộ (trong app) và liên ngân hàng (giả lập). Xác nhận bằng PIN/OTP, hiển thị biên lai.
- **Entity liên quan**: `Account`, `Transaction`, `OtpCode`, `Notification`

## Chức năng con
1. **Chuyển nội bộ**: nhập số tài khoản đích → kiểm tra tồn tại → nhập tiền + nội dung → xác nhận PIN/OTP → thực hiện.
2. **Chuyển liên ngân hàng (giả lập)**: nhập số tài khoản ngoài + mã ngân hàng → mô phỏng xử lý.
3. **Biên lai**: hiển thị sau khi thành công, gồm `reference_code`.

## Quy tắc nghiệp vụ (QUAN TRỌNG)
- Toàn bộ thao tác trừ/cộng số dư + ghi `Transaction` nằm trong **một** `@Transactional`.
- Chống race condition: khóa pessimistic (`SELECT ... FOR UPDATE`) hoặc optimistic lock trên `accounts`.
- Kiểm tra trước khi chuyển: số dư đủ, `amount > 0`, tài khoản không bị `locked`, không tự chuyển cho chính mình.
- Sai PIN/OTP → từ chối, không thay đổi số dư.
- Nội bộ: trừ `from`, cộng `to`, status `success`.
- Liên ngân hàng: chỉ trừ `from`, ghi `to_external_account`+`to_bank_code`, mô phỏng kết quả.
- Thành công → sinh `Notification` type `transaction` cho cả hai bên (nếu nội bộ).
- Nếu lỗi giữa chừng → rollback toàn bộ, status `failed`, không đổi số dư.

## Schema dữ liệu sử dụng
- Cập nhật `accounts.balance` (from & to).
- Ghi `transactions` (type `internal`/`interbank`, reference_code duy nhất).
- Đọc `otp_codes` / `users.pin_hash` để xác thực.
- Ghi `notifications`.

## Gợi ý endpoint
- `POST /transfer/internal/validate`  (kiểm tra tài khoản đích)
- `POST /transfer/internal`
- `POST /transfer/interbank`
- `GET  /transfer/receipt/{referenceCode}`
