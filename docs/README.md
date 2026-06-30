# Banking App — API Docs

> **Base URL:** `http://localhost:8080`  
> **Content-Type:** `application/json` (trừ upload dùng `multipart/form-data`)

## Danh sách nhóm API

| File | Nhóm | Endpoint gốc |
|------|------|-------------|
| [api-auth.md](api-auth.md) | Xác thực | `/auth` |
| [api-profile.md](api-profile.md) | Hồ sơ người dùng | `/profile` |
| [api-dashboard.md](api-dashboard.md) | Dashboard | `/dashboard` |
| [api-notifications.md](api-notifications.md) | Thông báo | `/notifications` |
| [api-transfer.md](api-transfer.md) | Chuyển tiền | `/transfers` |
| [api-transactions.md](api-transactions.md) | Giao dịch (biên lai + lịch sử) | `/transactions` |
| [api-topup.md](api-topup.md) | Nạp tiền điện thoại | `/topup` |
| [api-cards.md](api-cards.md) | Quản lý thẻ ảo | `/cards` |
| [api-qr.md](api-qr.md) | Mã QR VietQR | `/qr` |
| [api-savings.md](api-savings.md) | Tiết kiệm | `/savings` |
| [api-devices.md](api-devices.md) | Thiết bị & Đăng xuất từ xa | `/devices` |
| [api-reports.md](api-reports.md) | Báo cáo & Biểu đồ | `/reports` |

---

## Response wrapper

Mọi response đều bọc trong `ApiResponse`:

```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": { ... }
}
```

Khi lỗi, `data` bị bỏ qua:

```json
{
  "success": false,
  "message": "Email hoặc mật khẩu không đúng"
}
```

Lỗi validation trả về `data` dạng map field → message:

```json
{
  "success": false,
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": {
    "email": "Email không hợp lệ",
    "password": "Mật khẩu phải từ 8–100 ký tự"
  }
}
```

---

## Xác thực (JWT)

Các endpoint có nhãn **🔒 JWT** yêu cầu header:

```
Authorization: Bearer <token>
```

Token có hiệu lực **24 giờ** (`expiresIn` = 86400 giây).

| Trường hợp | HTTP |
|-----------|------|
| Thiếu / hết hạn token | `401 Unauthorized` |
| Đúng token nhưng không có quyền | `403 Forbidden` |

---

## Dev mode — `devOtpCode`

Khi server chạy với `app.otp.dev-mode=true` (môi trường development), tất cả response trả OTP đều kèm thêm field:

```json
"devOtpCode": "482910"
```

Field này **không xuất hiện trong production**. Dùng để test mà không cần nhận email thật.

---

## Enum values

| Enum | Giá trị |
|------|---------|
| `OtpPurpose` | `REGISTER`, `LOGIN`, `TRANSFER`, `RESET_PASSWORD` |
| `AccountType` | `PAYMENT`, `SAVING` |
| `AccountStatus` | `ACTIVE`, `LOCKED` |
| `UserStatus` | `ACTIVE`, `LOCKED`, `PENDING` |
| `TransactionType` | `INTERNAL`, `INTERBANK`, `TOPUP`, `SAVINGS_DEPOSIT`, `SAVINGS_WITHDRAW` |
| `TransactionStatus` | `PENDING`, `SUCCESS`, `FAILED` |
| `NotificationType` | `TRANSACTION`, `BALANCE`, `SYSTEM` |
| `SavingsStatus` | `ACTIVE`, `MATURED`, `WITHDRAWN` |
| `CardStatus` | `ACTIVE`, `LOCKED` |
| `TopupCarrier` | `VIETTEL`, `MOBIFONE`, `VINAPHONE`, `VIETNAMOBILE`, `ITEL` |
| `ReportPeriod` | `MONTH`, `WEEK` |
| `ReportDirection` | `EXPENSE`, `INCOME` |

---

## HTTP Status Code tham chiếu

| Code | Ý nghĩa |
|------|---------|
| `200` | Thành công |
| `201` | Tạo mới thành công |
| `400` | Dữ liệu không hợp lệ / lỗi nghiệp vụ |
| `401` | Chưa xác thực (thiếu / hết hạn JWT) |
| `403` | Không có quyền |
| `404` | Không tìm thấy tài nguyên |
| `409` | Xung đột dữ liệu (email/phone đã tồn tại) |
| `500` | Lỗi hệ thống |

---

## Lưu ý endpoint trùng lặp (PIN & password)

Một số chức năng có mặt ở **cả hai nhóm** `/auth` và `/profile`. Khuyến nghị dùng nhóm `/profile` vì xử lý cả hai trường hợp (thiết lập lần đầu và thay đổi) trong một endpoint duy nhất:

| Chức năng | Dùng endpoint này | Ghi chú |
|-----------|------------------|---------|
| Đổi mật khẩu | `PUT /profile/password` | Tương đương `PUT /auth/password` |
| Thiết lập / đổi PIN | `PUT /profile/pin` | Thay thế cả `PUT /auth/pin` lẫn `PUT /auth/pin/change` |

`PUT /profile/pin` tự động phân biệt:
- Nếu user **chưa có PIN** → chỉ cần `newPin`
- Nếu user **đã có PIN** → bắt buộc `currentPin` + `newPin`

---

## Luồng sử dụng điển hình

### Đăng ký tài khoản mới

```
POST /auth/register              → nhận OTP qua email
POST /auth/verify-otp            → nhận JWT (tự động đăng nhập)
PUT  /profile/pin  {newPin}      → thiết lập PIN giao dịch (bắt buộc trước khi chuyển tiền)
```

### Đăng nhập

```
POST /auth/login                 → nhận JWT
GET  /dashboard/summary          → load màn hình home
```

### Quên mật khẩu

```
POST /auth/forgot-password       → nhận OTP qua email
POST /auth/reset-password        → đặt mật khẩu mới bằng OTP
POST /auth/login                 → đăng nhập lại
```

### Tải màn hình home

```
GET /dashboard/summary           → 1 request: tài khoản + số dư + giao dịch gần đây + badge
```

### Chuyển tiền nội bộ (hai bước)

```
POST /transfers/internal         → Bước 1: xác thực PIN + validate → nhận confirmToken + OTP
POST /transfers/confirm          → Bước 2: xác nhận OTP → giao dịch SUCCESS ngay
```

> confirmToken có hiệu lực **5 phút** và chỉ dùng được **một lần**.

### Chuyển tiền liên ngân hàng (hai bước + polling)

```
POST /transfers/interbank        → Bước 1: xác thực PIN + validate → nhận confirmToken + OTP
POST /transfers/confirm          → Bước 2: xác nhận OTP → giao dịch PENDING

[3–8 giây sau, hệ thống xử lý ngầm]

GET  /notifications              → polling để nhận kết quả SUCCESS hoặc FAILED
GET  /transactions/{id}/receipt  → xem biên lai sau khi có kết quả
```

> Cách đơn giản nhất: sau khi nhận về `status = PENDING`, poll `GET /notifications` mỗi 2–3 giây cho đến khi có thông báo mới về giao dịch đó. Hoặc gọi `GET /dashboard/summary` khi user quay lại màn hình home.

### Xem lịch sử giao dịch

```
GET /transactions/history?accountId=1&page=0&size=20
GET /transactions/{id}/receipt   → biên lai chi tiết
```

### Thông báo

```
GET /notifications/unread-count  → lấy số badge (gọi khi app được focus)
GET /notifications               → danh sách thông báo
PATCH /notifications/read-all    → đánh dấu tất cả đã đọc
```

### Sinh mã QR để nhận tiền

```
GET /qr/generate?accountId=1                               → QR không gắn số tiền
GET /qr/generate?accountId=1&amount=150000&description=... → QR có số tiền cố định
→ dùng qrContent vẽ ảnh QR bằng ZXing / QRCode-Kotlin
```

### Đăng nhập kèm thiết bị (khuyến nghị)

```
POST /auth/login { credential, password, deviceId, deviceName, pushToken }
→ Server tự upsert thiết bị + trả JWT nhúng claim "did"
→ Không cần gọi POST /devices riêng
```

### Tích hợp FCM push notification

```
[Sau mỗi lần đăng nhập]
POST /devices { deviceId, deviceName, pushToken }   → upsert token

[Khi onNewToken() được gọi bởi Firebase SDK]
POST /devices { deviceId, pushToken }               → cập nhật token mới

[Khi đăng xuất]
DELETE /devices/{id}                                → xóa token, ngừng nhận push
                                                       + vô hiệu hóa JWT ngay lập tức
```

### Quản lý thiết bị & Đăng xuất từ xa

```
GET /devices                → danh sách thiết bị (active + inactive)
DELETE /devices/{id}        → đăng xuất thiết bị bất kỳ từ xa
                              → JWT của thiết bị đó bị từ chối ngay (401)
```

### Nạp tiền điện thoại (hai bước)

```
GET /topup/carriers                      → dropdown nhà mạng
GET /topup/face-values                   → danh sách mệnh giá

POST /topup/initiate { fromAccountId, phoneNumber, carrier, faceValue, pin }
→ Bước 1: xác thực PIN → nhận confirmToken + OTP

POST /topup/confirm { confirmToken, otpCode }
→ Bước 2: xác nhận OTP → trừ tiền + ghi giao dịch
```

### Quản lý thẻ ảo

```
GET  /cards                             → danh sách thẻ
POST /cards { accountId }              → phát hành thẻ mới (tối đa 5/tài khoản)
PUT  /cards/{id}/lock                  → khóa / mở khóa (toggle)
PUT  /cards/{id}/limit { dailyLimit }  → đặt hạn mức (null = bỏ hạn mức)
```

### Mở và quản lý tiết kiệm

```
POST /savings { fromAccountId, amount, termMonths, pin }  → mở sổ, trừ tiền ngay
GET  /savings                                             → danh sách sổ
GET  /savings/{id}                                        → chi tiết + preview rút sớm
POST /savings/{id}/withdraw { pin }                       → tất toán, nhận tiền về
```

### Xem báo cáo chi tiêu

```
GET /reports/spending?period=MONTH&from=2024-01-01&to=2024-12-31
→ Thống kê thu/chi thô theo tháng/tuần

GET /reports/chart/bar?period=MONTH&from=...&to=...
→ Dữ liệu biểu đồ cột (labels + series arrays, index-aligned)

GET /reports/chart/pie?direction=EXPENSE&from=...&to=...
→ Dữ liệu biểu đồ tròn (slices theo loại giao dịch + màu sắc)
```
