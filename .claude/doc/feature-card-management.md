# Feature: Card Management (Quản lý thẻ)

- **Phần**: 3 (Bổ sung)
- **Mô tả**: Hiển thị thẻ ảo (số thẻ, ngày hết hạn); khóa/mở thẻ tạm thời; đặt hạn mức giao dịch.
- **Entity liên quan**: `Card`, `Account`

## Chức năng con
1. **Xem thẻ**: hiển thị danh sách thẻ ảo của user (số thẻ che bớt, ngày hết hạn, tên chủ thẻ).
2. **Khóa/mở thẻ**: đổi `status` giữa `active` và `locked`.
3. **Đặt hạn mức**: cập nhật `daily_limit`.
4. **(Tùy chọn) Tạo thẻ ảo**: phát hành thẻ mới gắn với tài khoản.

## Quy tắc nghiệp vụ
- Mỗi thẻ thuộc về một `account` của user đang đăng nhập (kiểm tra quyền sở hữu).
- Số thẻ hiển thị dạng che: chỉ lộ 4 số cuối (`**** **** **** 1234`).
- Khóa thẻ: `status = 'locked'` → thẻ không dùng để giao dịch; mở lại đặt `active`.
- `daily_limit`: nếu có, mọi giao dịch qua thẻ trong ngày không được vượt; NULL = không giới hạn.
- Đây là thẻ mô phỏng — không xử lý thanh toán thẻ thật.

## Schema dữ liệu sử dụng
- Đọc/ghi `cards` (status, daily_limit).
- Đọc `accounts` (kiểm tra quyền sở hữu).

## Gợi ý endpoint
- `GET  /cards`                 (danh sách thẻ)
- `POST /cards`                 (tạo thẻ ảo — tùy chọn)
- `PUT  /cards/{id}/lock`       (khóa/mở)
- `PUT  /cards/{id}/limit`      (đặt hạn mức)
