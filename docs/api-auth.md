# Auth API — `/auth`

> Xem quy ước chung tại [README.md](README.md)

| Endpoint | Method | Auth | Mô tả |
|----------|--------|------|-------|
| `/auth/register` | POST | Public | Đăng ký, nhận OTP |
| `/auth/verify-otp` | POST | Public | Xác thực OTP, nhận token |
| `/auth/login` | POST | Public | Đăng nhập |
| `/auth/forgot-password` | POST | Public | Gửi OTP đặt lại mật khẩu |
| `/auth/reset-password` | POST | Public | Đặt lại mật khẩu bằng OTP |
| `/auth/resend-otp` | POST | Public | Gửi lại OTP |
| `/auth/password` | PUT | 🔒 JWT | Đổi mật khẩu _(xem `/profile/password`)_ |
| `/auth/pin` | PUT | 🔒 JWT | Thiết lập PIN lần đầu _(xem `/profile/pin`)_ |
| `/auth/pin/change` | PUT | 🔒 JWT | Đổi PIN _(xem `/profile/pin`)_ |

> **Khuyến nghị:** Với PIN và mật khẩu, ưu tiên dùng các endpoint tương ứng trong `/profile` — chúng xử lý cả hai trường hợp (thiết lập lần đầu và thay đổi) trong một endpoint duy nhất. Xem [README.md § Lưu ý endpoint trùng lặp](README.md#lưu-ý-endpoint-trùng-lặp-pin--password) và [api-profile.md](api-profile.md).

---

## POST /auth/register

Bước 1 đăng ký: gửi thông tin → nhận OTP xác thực qua email.

**Request**

```json
{
  "fullName": "Nguyễn Văn A",
  "email": "user@example.com",
  "phone": "0901234567",
  "password": "Password123!"
}
```

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `fullName` | ✓ | tối đa 100 ký tự |
| `email` | ✓ | định dạng email hợp lệ |
| `phone` | ✓ | 10 chữ số, bắt đầu bằng `0` |
| `password` | ✓ | 8–100 ký tự |

**Response `201`**

```json
{
  "success": true,
  "message": "OTP đã được gửi, vui lòng xác thực để hoàn tất đăng ký",
  "data": {
    "channel": "EMAIL",
    "target": "us***@example.com",
    "expiresInSeconds": 300,
    "devOtpCode": "482910"
  }
}
```

> `devOtpCode` chỉ có khi `app.otp.dev-mode=true`. Không xuất hiện trong production.

**Lỗi**

| HTTP | message |
|------|---------|
| `409` | Email đã được sử dụng |
| `409` | Số điện thoại đã được sử dụng |
| `400` | Dữ liệu đầu vào không hợp lệ |

---

## POST /auth/verify-otp

Bước 2 đăng ký: xác thực OTP → nhận JWT (tự động đăng nhập).

**Request**

```json
{
  "credential": "user@example.com",
  "code": "482910"
}
```

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `credential` | ✓ | email hoặc số điện thoại |
| `code` | ✓ | đúng 6 ký tự |

**Response `200`**

```json
{
  "success": true,
  "message": "Đăng ký thành công",
  "data": {
    "token": "eyJhbGci...",
    "userId": 1,
    "email": "user@example.com",
    "fullName": "Nguyễn Văn A",
    "expiresIn": 86400
  }
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `400` | Mã OTP không hợp lệ hoặc đã hết hạn |
| `404` | Không tìm thấy người dùng |

---

## POST /auth/login

**Request**

```json
{
  "credential": "user@example.com",
  "password": "Password123!",
  "deviceId": "a1b2c3d4e5f6a1b2",
  "deviceName": "Samsung Galaxy S24",
  "pushToken": "fGH5k9LmN2pQ:APA91bH..."
}
```

> `credential` chấp nhận cả **email** lẫn **số điện thoại**.

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `credential` | ✓ | Email hoặc số điện thoại |
| `password` | ✓ | Mật khẩu |
| `deviceId` | — | `Settings.Secure.ANDROID_ID` của thiết bị. Nếu gửi kèm, JWT trả về sẽ nhúng claim `did` để bật kiểm tra thiết bị trên mỗi request |
| `deviceName` | — | Tên thiết bị hiển thị (model máy). Tối đa 100 ký tự |
| `pushToken` | — | FCM registration token. Tối đa 255 ký tự |

> **Khuyến nghị Android:** Luôn gửi `deviceId` + `pushToken` khi login. Server sẽ upsert thiết bị tự động — không cần gọi `POST /devices` riêng sau đó.

**Response `200`**

```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "token": "eyJhbGci...",
    "userId": 1,
    "email": "user@example.com",
    "fullName": "Nguyễn Văn A",
    "expiresIn": 86400
  }
}
```

> Nếu đã gửi `deviceId`, JWT được nhúng thêm claim `"did": "<deviceId>"`. Token này sẽ bị từ chối ngay (401) nếu thiết bị bị đăng xuất từ xa qua `DELETE /devices/{id}`. Xem [api-devices.md § Đăng xuất từ xa](api-devices.md#đăng-xuất-từ-xa--remote-logout).

**Lỗi**

| HTTP | message |
|------|---------|
| `401` | Email hoặc mật khẩu không đúng |
| `403` | Tài khoản người dùng đã bị khóa |

---

## POST /auth/forgot-password

**Request**

```json
{
  "credential": "user@example.com"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "OTP đặt lại mật khẩu đã được gửi",
  "data": {
    "channel": "EMAIL",
    "target": "us***@example.com",
    "expiresInSeconds": 300
  }
}
```

---

## POST /auth/reset-password

**Request**

```json
{
  "credential": "user@example.com",
  "otpCode": "123456",
  "newPassword": "NewPassword456!"
}
```

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `credential` | ✓ | email hoặc số điện thoại |
| `otpCode` | ✓ | 6 ký tự |
| `newPassword` | ✓ | 8–100 ký tự |

**Response `200`**

```json
{
  "success": true,
  "message": "Mật khẩu đã được đặt lại thành công"
}
```

---

## POST /auth/resend-otp

**Request**

```json
{
  "credential": "user@example.com",
  "purpose": "REGISTER"
}
```

> `purpose`: `REGISTER` hoặc `RESET_PASSWORD`

**Response `200`** — cấu trúc giống `/auth/register`.

---

## PUT /auth/password 🔒

**Request**

```json
{
  "currentPassword": "Password123!",
  "newPassword": "NewPassword456!"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Đổi mật khẩu thành công"
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `400` | Mật khẩu hiện tại không đúng |

---

## PUT /auth/pin 🔒

Thiết lập PIN **lần đầu** (chưa có PIN).

**Request**

```json
{
  "pin": "123456"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Thiết lập PIN thành công"
}
```

---

## PUT /auth/pin/change 🔒

Đổi PIN (đã có PIN trước đó).

**Request**

```json
{
  "currentPin": "123456",
  "newPin": "654321"
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Đổi PIN thành công"
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `400` | Mã PIN không đúng |
