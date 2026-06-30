# Feature: Push Notification

- **Phần**: 2 (Nâng cao)
- **Mô tả**: Thông báo đẩy nhắc nhở giao dịch và biến động số dư qua Firebase Cloud Messaging.
- **Entity liên quan**: `Device`, `Notification`

## Chức năng con
1. **Đăng ký token**: client gửi FCM token → lưu vào `devices.push_token`.
2. **Gửi push khi giao dịch**: sau mỗi giao dịch thành công, đẩy thông báo.
3. **Gửi push khi biến động số dư**: khi balance thay đổi (cộng/trừ).

## Quy tắc nghiệp vụ
- Lưu/ cập nhật `push_token` theo từng `device` của user.
- Khi cần gửi: lấy `push_token` của các device `is_active = true` thuộc user.
- Nội dung push đồng bộ với bản ghi `notifications` (lưu in-app + đẩy push cùng lúc).
- Tích hợp Firebase Admin SDK phía backend để gửi.
- Xử lý token hết hạn/không hợp lệ (FCM trả lỗi) → có thể bỏ qua hoặc đánh dấu.

## Schema dữ liệu sử dụng
- Đọc/ghi `devices` (push_token, is_active).
- Ghi `notifications` (title, content, type).

## Gợi ý endpoint
- `POST /devices/register-token`
- (gửi push là tác vụ nội bộ, kích hoạt từ service Transfer/Savings)
