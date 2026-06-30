# Feature: Account Management (Quản lý tài khoản cá nhân)

- **Phần**: 1 (Core)
- **Mô tả**: Xem thông tin cá nhân, đổi mật khẩu, đổi PIN, cập nhật ảnh đại diện.
- **Entity liên quan**: `User`, `Account`

## Chức năng con
1. **Xem thông tin**: họ tên, email, phone, avatar, danh sách tài khoản + số dư.
2. **Đổi mật khẩu**: xác minh mật khẩu cũ → đặt mật khẩu mới (bcrypt).
3. **Đổi PIN**: xác minh PIN cũ (nếu có) → đặt PIN mới (bcrypt).
4. **Cập nhật avatar**: upload ảnh → lưu file → cập nhật `avatar_url`.

## Quy tắc nghiệp vụ
- Mọi thao tác chỉ áp dụng cho user đang đăng nhập (theo JWT).
- Đổi mật khẩu/PIN bắt buộc xác minh giá trị cũ trước.
- Mật khẩu & PIN mới hash bcrypt; không trả hash về client.
- Avatar: giới hạn định dạng (jpg/png) và dung lượng.

## Schema dữ liệu sử dụng
- Đọc/ghi `users` (full_name, avatar_url, password_hash, pin_hash).
- Đọc `accounts` (hiển thị danh sách + số dư).

## Gợi ý endpoint
- `GET /profile`
- `PUT /profile/password`
- `PUT /profile/pin`
- `POST /profile/avatar`
