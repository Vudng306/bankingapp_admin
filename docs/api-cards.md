# Card Management API — `/cards`

> Xem quy ước chung tại [README.md](README.md)  
> Tất cả endpoint yêu cầu **🔒 JWT**

## Tổng quan

API quản lý thẻ ảo (virtual card) — thẻ Visa-style sinh tự động, dùng để thanh toán online.

- Mỗi tài khoản tối đa **5 thẻ**
- Số thẻ 16 chữ số, tiền tố `4` (Visa-style), được sinh ngẫu nhiên và đảm bảo unique
- Ngày hết hạn = ngày phát hành + 3 năm, hiển thị dạng `MM/yy`
- Thẻ có thể **khóa/mở khóa** và **đặt hạn mức giao dịch hàng ngày**

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/cards` | GET | Danh sách thẻ của user |
| `/cards` | POST | Phát hành thẻ ảo mới |
| `/cards/{id}/lock` | PUT | Khóa / Mở khóa thẻ (toggle) |
| `/cards/{id}/limit` | PUT | Thiết lập hạn mức giao dịch hàng ngày |

---

## GET /cards

Danh sách tất cả thẻ của user (qua mọi tài khoản), mới nhất trước.

**Response `200`**

```json
{
  "success": true,
  "message": "Danh sách thẻ",
  "data": [
    {
      "id": 1,
      "maskedNumber": "**** **** **** 9082",
      "expiryDate": "06/29",
      "cardholderName": "NGUYEN VAN A",
      "status": "ACTIVE",
      "dailyLimit": 5000000.00,
      "accountNumber": "9704001805473643",
      "createdAt": "2026-06-29T22:15:08"
    }
  ]
}
```

**Cấu trúc từng phần tử**

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | Long | ID thẻ — dùng cho các endpoint lock/limit |
| `maskedNumber` | String | Số thẻ đã che, 4 số cuối hiển thị |
| `expiryDate` | String | Ngày hết hạn dạng `MM/yy` |
| `cardholderName` | String | Tên chủ thẻ (IN HOA) |
| `status` | String | `ACTIVE` hoặc `LOCKED` |
| `dailyLimit` | Number | Hạn mức hàng ngày (VND). `null` = không giới hạn |
| `accountNumber` | String | Số tài khoản gắn với thẻ |
| `createdAt` | DateTime | Ngày phát hành thẻ |

---

## POST /cards

Phát hành thẻ ảo mới cho một tài khoản.

**Request**

```json
{
  "accountId": 1
}
```

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `accountId` | ✓ | ID tài khoản PAYMENT thuộc user đang đăng nhập |

**Response `200`**

```json
{
  "success": true,
  "message": "Tạo thẻ ảo thành công",
  "data": {
    "id": 1,
    "maskedNumber": "**** **** **** 9082",
    "expiryDate": "06/29",
    "cardholderName": "NGUYEN VAN A",
    "status": "ACTIVE",
    "dailyLimit": null,
    "accountNumber": "9704001805473643",
    "createdAt": "2026-06-29T22:15:08"
  }
}
```

**Lỗi**

| HTTP | message | Nguyên nhân |
|------|---------|-------------|
| `400` | Tài khoản đã đạt giới hạn số thẻ (tối đa 5) | Tài khoản đã có 5 thẻ |
| `400` | Tài khoản đã bị khóa | Tài khoản không ACTIVE |
| `403` | Không có quyền thực hiện thao tác này | `accountId` không thuộc user |
| `404` | Không tìm thấy tài khoản | `accountId` không tồn tại |

---

## PUT /cards/{id}/lock

Khóa hoặc mở khóa thẻ — **toggle**: gọi 1 lần thì khóa, gọi lần nữa thì mở.

**Path param**

| Param | Mô tả |
|-------|-------|
| `id` | ID thẻ (lấy từ GET /cards) |

**Request body:** Không cần.

**Response `200`** — trả về trạng thái mới của thẻ

```json
{
  "success": true,
  "message": "Đã khóa thẻ",
  "data": {
    "id": 1,
    "maskedNumber": "**** **** **** 9082",
    "expiryDate": "06/29",
    "cardholderName": "NGUYEN VAN A",
    "status": "LOCKED",
    "dailyLimit": null,
    "accountNumber": "9704001805473643",
    "createdAt": "2026-06-29T22:15:08"
  }
}
```

> `message` tự động thay đổi theo trạng thái: `"Đã khóa thẻ"` hoặc `"Đã mở khóa thẻ"`.

**Lỗi**

| HTTP | message |
|------|---------|
| `404` | Không tìm thấy thẻ |

---

## PUT /cards/{id}/limit

Thiết lập hạn mức giao dịch hàng ngày. Gửi `null` để bỏ giới hạn.

**Path param**

| Param | Mô tả |
|-------|-------|
| `id` | ID thẻ |

**Request**

```json
{
  "dailyLimit": 5000000
}
```

Để bỏ hạn mức:

```json
{
  "dailyLimit": null
}
```

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `dailyLimit` | — | `null` (bỏ hạn mức) hoặc số dương > 0. Giá trị 0 hoặc âm bị từ chối |

**Response `200`**

```json
{
  "success": true,
  "message": "Cập nhật hạn mức thành công",
  "data": {
    "id": 1,
    "maskedNumber": "**** **** **** 9082",
    "expiryDate": "06/29",
    "cardholderName": "NGUYEN VAN A",
    "status": "ACTIVE",
    "dailyLimit": 5000000,
    "accountNumber": "9704001805473643",
    "createdAt": "2026-06-29T22:15:08"
  }
}
```

> Khi `dailyLimit` là `null`, field bị bỏ qua trong response (`@JsonInclude(NON_NULL)`).

**Lỗi**

| HTTP | message | Nguyên nhân |
|------|---------|-------------|
| `400` | Hạn mức phải lớn hơn 0 | `dailyLimit <= 0` |
| `404` | Không tìm thấy thẻ | `id` không tồn tại hoặc không thuộc user |

---

## Luồng tích hợp

### Hiển thị danh sách thẻ

```
GET /cards
→ Render từng thẻ với maskedNumber, expiryDate, status, dailyLimit
→ Badge LOCKED nếu status = "LOCKED"
→ Nút "Khóa/Mở khóa" → PUT /cards/{id}/lock
→ Nút "Đặt hạn mức" → PUT /cards/{id}/limit
```

### Phát hành thẻ mới

```
1. Hiển thị tài khoản của user (từ GET /dashboard/summary)
2. User chọn tài khoản → POST /cards { accountId }
3. Hiển thị thẻ mới được tạo
```

### Quản lý thẻ

```
Khóa thẻ khi nghi bị lộ:
  PUT /cards/{id}/lock → status = LOCKED

Mở lại sau khi xác nhận an toàn:
  PUT /cards/{id}/lock → status = ACTIVE

Đặt hạn mức cho thẻ phụ:
  PUT /cards/{id}/limit { "dailyLimit": 2000000 }

Bỏ hạn mức (tiêu không giới hạn):
  PUT /cards/{id}/limit { "dailyLimit": null }
```
