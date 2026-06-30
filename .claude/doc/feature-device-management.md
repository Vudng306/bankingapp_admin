# Feature: Device Management (Quản lý thiết bị đăng nhập)

- **Phần**: 3 (Bổ sung)
- **Mô tả**: Xem danh sách thiết bị đã đăng nhập, cho phép đăng xuất từ xa.
- **Entity liên quan**: `Device` (tái dùng bảng `devices` từ Phần 2)

## Chức năng con
1. **Danh sách thiết bị**: hiển thị các thiết bị đã/đang đăng nhập của user (tên, lần đăng nhập gần nhất, trạng thái).
2. **Đăng xuất từ xa**: vô hiệu hóa một thiết bị (`is_active = false`).
3. **(Liên quan) Đánh dấu thiết bị hiện tại**: phân biệt thiết bị đang dùng với các thiết bị khác.

## Quy tắc nghiệp vụ
- Mỗi lần đăng nhập: tạo mới hoặc cập nhật bản ghi `devices` (device_id, device_name, last_login_at, is_active=true).
- Đăng xuất từ xa: đặt `is_active = false`; thiết bị đó cần bị từ chối ở lần gọi API tiếp theo.
- Để đăng xuất từ xa có hiệu lực thật: token JWT của thiết bị bị vô hiệu nên được kiểm tra đối chiếu `is_active` (vd middleware kiểm tra device còn active không), hoặc dùng danh sách token thu hồi.
- Chỉ thao tác trên thiết bị thuộc user đang đăng nhập.
- Không lưu dữ liệu sinh trắc học; `biometric_enabled` chỉ là cờ bật/tắt.

## Schema dữ liệu sử dụng
- Đọc/ghi `devices` (is_active, last_login_at, device_name, device_id).

## Gợi ý endpoint
- `GET    /devices`             (danh sách thiết bị của user)
- `DELETE /devices/{id}`        (đăng xuất từ xa — đặt is_active=false)
