# Phone Topup API — `/topup`

> Xem quy ước chung tại [README.md](README.md)  
> Tất cả endpoint yêu cầu **🔒 JWT**

## Luồng hai bước (PIN → OTP → Execute)

Nạp tiền điện thoại dùng cùng cơ chế 2 bước như chuyển tiền:

```
Bước 1 — Initiate          Bước 2 — Confirm
─────────────────           ─────────────────
POST /topup/initiate        POST /topup/confirm
```

```
Client                    Server
  │                          │
  ├─ POST /topup/initiate ───▶ Xác thực PIN
  │  { fromAccountId,          Validate: nhà mạng, mệnh giá, số dư
  │    phoneNumber,            Lưu TransferSession (TTL 5 phút)
  │    carrier, faceValue,     Gửi OTP qua Email
  │    pin }
  │◀─ 200 { confirmToken, otpResponse, ... }
  │
  ├─ POST /topup/confirm ─────▶ Xác thực OTP
  │  { confirmToken, otpCode }  Trừ số dư (pessimistic lock)
  │                             Lưu transactions + phone_topups
  │                             Ghi notification
  │◀─ 200 { TopupReceiptResponse }
```

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/topup/carriers` | GET | Danh sách nhà mạng hỗ trợ |
| `/topup/face-values` | GET | Danh sách mệnh giá |
| `/topup/initiate` | POST | Bước 1: xác thực PIN, tạo phiên, gửi OTP |
| `/topup/confirm` | POST | Bước 2: xác nhận OTP, thực thi nạp tiền |

---

## GET /topup/carriers

Danh sách nhà mạng được hỗ trợ. Dùng để render dropdown chọn nhà mạng.

**Response `200`**

```json
{
  "success": true,
  "message": "Danh sách nhà mạng",
  "data": [
    { "id": "VIETTEL",     "name": "Viettel" },
    { "id": "MOBIFONE",    "name": "MobiFone" },
    { "id": "VINAPHONE",   "name": "VinaPhone" },
    { "id": "VIETNAMOBILE","name": "Vietnamobile" },
    { "id": "ITEL",        "name": "Itelecom" }
  ]
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | String | Mã nhà mạng — dùng làm giá trị `carrier` khi gọi `/topup/initiate` |
| `name` | String | Tên hiển thị |

---

## GET /topup/face-values

Danh sách mệnh giá nạp tiền được hỗ trợ. Dùng để render danh sách lựa chọn.

**Response `200`**

```json
{
  "success": true,
  "message": "Danh sách mệnh giá",
  "data": [
    { "amount": 10000,  "label": "10.000đ" },
    { "amount": 20000,  "label": "20.000đ" },
    { "amount": 50000,  "label": "50.000đ" },
    { "amount": 100000, "label": "100.000đ" },
    { "amount": 200000, "label": "200.000đ" },
    { "amount": 500000, "label": "500.000đ" }
  ]
}
```

| Field | Type | Mô tả |
|-------|------|-------|
| `amount` | Number | Mệnh giá bằng VND — dùng làm giá trị `faceValue` khi gọi `/topup/initiate` |
| `label` | String | Chuỗi hiển thị đã được format |

---

## POST /topup/initiate

Bước 1 — Xác thực PIN, validate thông tin, gửi OTP. **Chưa trừ số dư.**

**Request**

```json
{
  "fromAccountId": 1,
  "phoneNumber": "0912345678",
  "carrier": "VIETTEL",
  "faceValue": 50000,
  "pin": "123456"
}
```

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `fromAccountId` | ✓ | ID tài khoản nguồn (phải thuộc user đang đăng nhập) |
| `phoneNumber` | ✓ | Định dạng: `0[3-9]xxxxxxxx` hoặc `+84[3-9]xxxxxxxx` |
| `carrier` | ✓ | Một trong: `VIETTEL`, `MOBIFONE`, `VINAPHONE`, `VIETNAMOBILE`, `ITEL` |
| `faceValue` | ✓ | Phải là một trong các mệnh giá hợp lệ (lấy từ GET /topup/face-values) |
| `pin` | ✓ | PIN giao dịch 6 chữ số |

**Response `200`**

```json
{
  "success": true,
  "message": "Vui lòng nhập mã OTP để xác nhận",
  "data": {
    "confirmToken": "42527bfb-9de5-4ad0-8137-331b83c26f37",
    "otpResponse": {
      "channel": "EMAIL",
      "target": "le***@gmail.com",
      "expiresInSeconds": 300,
      "devOtpCode": "988891"
    },
    "fromAccountNumber": "9704001805473643",
    "fromAccountName": "Nguyễn Văn A",
    "phoneNumber": "0912345678",
    "carrier": "VIETTEL",
    "carrierName": "Viettel",
    "faceValue": 50000,
    "faceValueLabel": "50.000đ"
  }
}
```

**Cấu trúc `data`**

| Field | Type | Mô tả |
|-------|------|-------|
| `confirmToken` | String (UUID) | Token phiên — dùng ở Bước 2, có hiệu lực **5 phút**, dùng **một lần** |
| `otpResponse` | Object | Thông tin OTP đã gửi |
| `fromAccountNumber` | String | Số tài khoản nguồn |
| `fromAccountName` | String | Tên chủ tài khoản |
| `phoneNumber` | String | Số điện thoại cần nạp |
| `carrier` | String | Mã nhà mạng |
| `carrierName` | String | Tên nhà mạng hiển thị |
| `faceValue` | Number | Mệnh giá (VND) |
| `faceValueLabel` | String | Mệnh giá đã format |

**Lỗi**

| HTTP | message | Nguyên nhân |
|------|---------|-------------|
| `400` | Mã PIN không đúng | PIN sai |
| `400` | Người dùng chưa thiết lập PIN | Chưa có PIN |
| `400` | Nhà mạng không hợp lệ | `carrier` không thuộc danh sách |
| `400` | Mệnh giá không hợp lệ | `faceValue` không thuộc danh sách |
| `400` | Số dư tài khoản không đủ | Số dư < mệnh giá |
| `400` | Tài khoản đã bị khóa | Tài khoản nguồn không ACTIVE |
| `403` | Không có quyền thực hiện thao tác này | `fromAccountId` không thuộc user |
| `404` | Không tìm thấy tài khoản | `fromAccountId` không tồn tại |

---

## POST /topup/confirm

Bước 2 — Xác thực OTP, thực thi nạp tiền. **Trừ số dư tại đây.**

**Request**

```json
{
  "confirmToken": "42527bfb-9de5-4ad0-8137-331b83c26f37",
  "otpCode": "988891"
}
```

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `confirmToken` | ✓ | UUID từ Bước 1 |
| `otpCode` | ✓ | Đúng 6 ký tự |

**Response `200`**

```json
{
  "success": true,
  "message": "Nạp tiền thành công",
  "data": {
    "id": 13,
    "referenceCode": "TOP202606299621892",
    "status": "SUCCESS",
    "fromAccountNumber": "9704001805473643",
    "fromAccountName": "Nguyễn Văn A",
    "phoneNumber": "0912345678",
    "carrier": "VIETTEL",
    "carrierName": "Viettel",
    "faceValue": 50000.00,
    "faceValueLabel": "50.000đ",
    "description": "Nạp 50.000đ → 0912345678 (Viettel)",
    "createdAt": "2026-06-29T22:17:54"
  }
}
```

**Cấu trúc `data`**

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | Long | ID giao dịch — dùng để xem biên lai `/transactions/{id}/receipt` |
| `referenceCode` | String | Mã tham chiếu (format `TOPyyyyMMdd` + 7 số) |
| `status` | String | Luôn là `SUCCESS` |
| `fromAccountNumber` | String | Số TK bị trừ tiền |
| `fromAccountName` | String | Tên chủ tài khoản |
| `phoneNumber` | String | Số điện thoại đã nạp |
| `carrier` / `carrierName` | String | Nhà mạng |
| `faceValue` / `faceValueLabel` | Number / String | Mệnh giá |
| `description` | String | Mô tả giao dịch |
| `createdAt` | DateTime | Thời điểm giao dịch |

**Lỗi**

| HTTP | message | Nguyên nhân |
|------|---------|-------------|
| `400` | Mã OTP không hợp lệ hoặc đã hết hạn | OTP sai / đã dùng / hết hạn |
| `400` | Phiên giao dịch không hợp lệ | `confirmToken` sai / hết hạn 5 phút / đã dùng |
| `400` | Số dư tài khoản không đủ | Số dư thay đổi giữa 2 bước |

---

## Lưu ý tích hợp

- **confirmToken dùng một lần:** Sau khi confirm thành công, token không thể dùng lại. Nếu confirm thất bại (OTP sai), token **vẫn còn hiệu lực** trong 5 phút — user có thể thử lại với OTP đúng.
- **Thứ tự validate trong initiate:** PIN → carrier → faceValue → tài khoản → số dư. Sai PIN trả lỗi trước cả khi check nhà mạng.
- **Notification tự động:** Sau confirm thành công, server tự tạo notification loại `TRANSACTION` trong bảng notifications. Frontend nhận qua `GET /notifications`.
- **Luồng hiển thị:** Sau confirm, hiển thị màn hình biên lai bằng data trả về (không cần gọi thêm API). Nếu cần xem lại, dùng `GET /transactions/{id}/receipt`.
