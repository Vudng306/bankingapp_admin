# Feature: Dashboard

- **Phần**: 1 (Core)
- **Mô tả**: Trang chủ sau đăng nhập. Hiển thị số dư, giao dịch gần đây, thông báo.
- **Entity liên quan**: `Account`, `Transaction`, `Notification`

## Chức năng con
1. **Số dư**: tổng/từng tài khoản của user.
2. **Giao dịch gần đây**: N giao dịch mới nhất (vd 5-10).
3. **Thông báo**: số chưa đọc + danh sách rút gọn.

## Quy tắc nghiệp vụ
- Chỉ trả dữ liệu thuộc về user đang đăng nhập (lọc theo `userId` từ JWT).
- Giao dịch gần đây: lấy theo `created_at DESC`, gồm cả giao dịch gửi và nhận.
- Số dư đọc trực tiếp từ `accounts.balance` (không tính lại từ lịch sử).
- Badge thông báo = đếm `notifications.is_read = false`.

## Schema dữ liệu sử dụng
- Đọc `accounts` (balance).
- Đọc `transactions` (lọc theo account của user, sắp theo thời gian).
- Đọc `notifications` (đếm chưa đọc + list mới nhất).

## Gợi ý endpoint
- `GET /dashboard/summary`
- `GET /transactions/recent`
- `GET /notifications/unread-count`
