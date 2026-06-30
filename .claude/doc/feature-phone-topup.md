# Feature: Phone Topup (Nạp tiền điện thoại)

- **Phần**: 3 (Bổ sung)
- **Mô tả**: Chọn nhà mạng, nhập số điện thoại, chọn mệnh giá, thanh toán từ tài khoản.
- **Entity liên quan**: `PhoneTopup`, `Transaction`, `Account`

## Chức năng con
1. **Chọn nhà mạng**: hiển thị danh sách nhà mạng hỗ trợ.
2. **Nhập số + mệnh giá**: chọn từ danh sách mệnh giá cố định.
3. **Thanh toán**: trừ tiền từ tài khoản, xác nhận PIN/OTP.
4. **Kết quả**: hiển thị biên lai nạp tiền.

## Quy tắc nghiệp vụ
- Kiểm tra số dư đủ trước khi nạp.
- Trừ `face_value` khỏi `account` → ghi `Transaction` type `topup` → ghi `PhoneTopup`. Tất cả trong cùng `@Transactional`.
- Mệnh giá chỉ nhận giá trị trong danh sách cho phép (10k–500k).
- Xác thực PIN/OTP trước khi thực hiện (tái dùng cơ chế của Transfer).
- Mô phỏng: luôn thành công nếu số dư đủ (không gọi cổng nhà mạng thật).
- Sinh `Notification` sau khi nạp thành công.

## Schema dữ liệu sử dụng
- Cập nhật `accounts.balance`.
- Ghi `transactions` (type `topup`).
- Ghi `phone_topups` (carrier, phone_number, face_value).

## Gợi ý endpoint
- `GET  /topup/carriers`        (danh sách nhà mạng + mệnh giá)
- `POST /topup`                 (thực hiện nạp)
- `GET  /topup/receipt/{referenceCode}`
