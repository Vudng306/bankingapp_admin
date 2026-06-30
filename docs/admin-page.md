# Admin Page

Phần admin được thêm vào backend, không thêm bảng/cột DB mới. Admin đọc/ghi trực tiếp các entity/repository đang có:

- `users`: quản lý người dùng, trạng thái khóa/mở, kiểm tra PIN/avatar.
- `accounts`: quản lý tài khoản, số dư, trạng thái khóa/mở.
- `transactions`: lọc/tìm kiếm giao dịch, cập nhật trạng thái, xuất CSV/in PDF từ trình duyệt.
- `savings`: xem/cập nhật trạng thái sổ tiết kiệm.
- `cards`: khóa/mở thẻ, cập nhật hạn mức.
- `devices`: bật/tắt active để đăng xuất từ xa, bật/tắt biometric flag.
- `notifications`: tạo thông báo, đánh dấu đọc/chưa đọc.
- `phone_topups`: theo dõi nạp điện thoại.
- `otp_codes`, `transfer_sessions`: giám sát OTP và phiên xác nhận giao dịch.

## Truy cập

Chạy backend rồi mở:

```text
http://localhost:8080/admin
```

Tài khoản mặc định:

```text
admin / admin123
```

Nên đổi khi chạy demo/thật bằng biến môi trường hoặc `application.yml`:

```yaml
app:
  admin:
    username: ${ADMIN_USERNAME:admin}
    password: ${ADMIN_PASSWORD:admin123}
    session-minutes: ${ADMIN_SESSION_MINUTES:480}
```

## API admin chính

- `POST /admin-api/login`
- `GET /admin-api/summary`
- `GET /admin-api/users`, `GET /admin-api/users/{id}`, `PATCH /admin-api/users/{id}/status`
- `GET /admin-api/accounts`, `PATCH /admin-api/accounts/{id}/status`, `PATCH /admin-api/accounts/{id}/balance`
- `GET /admin-api/transactions`, `GET /admin-api/transactions/{id}`, `PATCH /admin-api/transactions/{id}/status`
- `GET /admin-api/savings`, `PATCH /admin-api/savings/{id}/status`
- `GET /admin-api/cards`, `PATCH /admin-api/cards/{id}`
- `GET /admin-api/devices`, `PATCH /admin-api/devices/{id}`
- `GET /admin-api/notifications`, `POST /admin-api/notifications`, `PATCH /admin-api/notifications/{id}/read`
- `GET /admin-api/topups`
- `GET /admin-api/otps`
- `GET /admin-api/transfer-sessions`

Sau khi login, các API admin cần header:

```text
X-Admin-Token: <token trả về từ /admin-api/login>
```

## Tính năng chưa thể quản lý đầy đủ vì DB hiện tại chưa có bảng

- Chuyển tiền theo lịch: cần bảng kiểu `scheduled_transfers`.
- Danh bạ người nhận/beneficiary: cần bảng kiểu `beneficiaries`.

Trong trang admin, hai mục này được hiển thị trong ma trận tính năng là “Thiếu DB”, đúng theo yêu cầu không sửa schema DB.
