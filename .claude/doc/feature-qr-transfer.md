# Feature: QR Transfer (Mã QR chuyển tiền)

- **Phần**: 2 (Nâng cao)
- **Mô tả**: Tạo mã QR cho tài khoản và quét QR để chuyển tiền.
- **Entity liên quan**: `Account`, `Transaction` (tái dùng luồng Transfer)

## Chức năng con
1. **Tạo QR cá nhân**: encode thông tin tài khoản (số tài khoản, tên chủ, có thể kèm số tiền cố định).
2. **Quét QR**: client quét → gửi nội dung lên server parse → điền sẵn form chuyển tiền.

## Quy tắc nghiệp vụ
- **Không cần bảng riêng**: QR chỉ encode dữ liệu từ `accounts`.
- Nội dung QR nên là chuỗi có cấu trúc (JSON hoặc định dạng tự định nghĩa): `account_number`, `account_holder_name`, `amount` (tùy chọn).
- Server validate tài khoản trong QR còn tồn tại và `active` trước khi cho chuyển.
- Sau khi parse QR → đi vào đúng luồng `Transfer` đã có (xác thực PIN/OTP, `@Transactional`).
- Thư viện gợi ý: ZXing để sinh/đọc QR.

## Schema dữ liệu sử dụng
- Đọc `accounts` (để sinh và validate QR).
- Dùng lại toàn bộ ghi `transactions` của feature Transfer.

## Gợi ý endpoint
- `GET  /qr/my-account`     (trả nội dung/ảnh QR của user)
- `POST /qr/parse`          (nhận chuỗi QR, trả thông tin người nhận)
