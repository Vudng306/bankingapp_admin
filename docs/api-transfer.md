# Transfer API — `/transfers`

> Xem quy ước chung tại [README.md](README.md)  
> Tất cả endpoint yêu cầu **🔒 JWT**

## Luồng hai bước (PIN → OTP → Execute)

Mọi giao dịch đều đi qua **hai bước**:

```
Bước 1 — Initiate          Bước 2 — Confirm
─────────────────           ─────────────────
POST /transfers/internal    POST /transfers/confirm
POST /transfers/interbank   (dùng chung cho cả hai loại)
```

```
Client                    Server
  │                          │
  ├─ POST /internal ─────────▶ Xác thực PIN
  │  { pin, ... }              Validate thông tin (số dư, tài khoản)
  │                            Lưu TransferSession (TTL 5 phút)
  │                            Gửi OTP qua Email
  │◀─ 200 { confirmToken, otpResponse }
  │
  ├─ POST /confirm ──────────▶ Xác thực OTP
  │  { confirmToken, otpCode }  Load phiên (kiểm tra TTL, dùng 1 lần)
  │                            Thực thi giao dịch (trừ số dư, lưu tx)
  │◀─ 200 { TransactionResponse }
```

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/transfers/internal` | POST | Bước 1: khởi tạo chuyển khoản nội bộ |
| `/transfers/interbank` | POST | Bước 1: khởi tạo chuyển khoản liên ngân hàng |
| `/transfers/confirm` | POST | Bước 2: xác nhận bằng OTP (dùng chung) |

---

## POST /transfers/internal

Bước 1 — Xác thực PIN, validate thông tin, gửi OTP. **Chưa trừ số dư.**

**Request**

```json
{
  "fromAccountId": 1,
  "toAccountNumber": "9704001928374650",
  "amount": 500000.00,
  "description": "Chuyển tiền ăn trưa",
  "pin": "123456"
}
```

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `fromAccountId` | ✓ | ID tài khoản nguồn (phải thuộc user đang đăng nhập) |
| `toAccountNumber` | ✓ | Số tài khoản đích trong hệ thống |
| `amount` | ✓ | Tối thiểu **1,000 VND** |
| `description` | — | Tối đa 255 ký tự |
| `pin` | ✓ | PIN giao dịch 6 chữ số |

**Response `200`**

```json
{
  "success": true,
  "message": "OTP xác nhận giao dịch đã được gửi",
  "data": {
    "confirmToken": "550e8400-e29b-41d4-a716-446655440000",
    "otpResponse": {
      "channel": "EMAIL",
      "target": "le***@gmail.com",
      "expiresInSeconds": 300
    }
  }
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `400` | Chưa thiết lập mã PIN giao dịch |
| `400` | Mã PIN không đúng |
| `400` | Số dư tài khoản không đủ |
| `400` | Không thể chuyển tiền vào chính tài khoản đó |
| `403` | Không có quyền thực hiện thao tác này |
| `403` | Tài khoản ngân hàng đã bị khóa |
| `404` | Không tìm thấy tài khoản |

---

## POST /transfers/interbank

Bước 1 — Xác thực PIN, validate thông tin, gửi OTP. **Chưa trừ số dư.**

**Request**

```json
{
  "fromAccountId": 1,
  "toBankCode": "VCB",
  "toAccountNumber": "0123456789",
  "toAccountName": "NGUYEN VAN B",
  "amount": 500000.00,
  "description": "Chuyển tiền học phí",
  "pin": "123456"
}
```

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `fromAccountId` | ✓ | ID tài khoản nguồn |
| `toBankCode` | ✓ | Mã ngân hàng (xem bảng bên dưới) |
| `toAccountNumber` | ✓ | Số tài khoản tại ngân hàng đích |
| `toAccountName` | ✓ | Tên chủ tài khoản đích, tối đa 100 ký tự |
| `amount` | ✓ | Tối thiểu **10,000 VND** |
| `description` | — | Tối đa 255 ký tự |
| `pin` | ✓ | PIN giao dịch 6 chữ số |

**Response `200`**

```json
{
  "success": true,
  "message": "OTP xác nhận giao dịch đã được gửi",
  "data": {
    "confirmToken": "550e8400-e29b-41d4-a716-446655440000",
    "otpResponse": {
      "channel": "EMAIL",
      "target": "le***@gmail.com",
      "expiresInSeconds": 300
    }
  }
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `400` | Mã ngân hàng không hợp lệ hoặc không được hỗ trợ |
| `400` | Chưa thiết lập mã PIN giao dịch |
| `400` | Mã PIN không đúng |
| `400` | Số dư tài khoản không đủ |
| `403` | Không có quyền / tài khoản bị khóa |
| `404` | Không tìm thấy tài khoản |

---

## POST /transfers/confirm

Bước 2 — Xác thực OTP + thực thi giao dịch. Dùng chung cho cả nội bộ và liên ngân hàng.

**Request**

```json
{
  "confirmToken": "550e8400-e29b-41d4-a716-446655440000",
  "otpCode": "847291"
}
```

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `confirmToken` | ✓ | UUID nhận được từ bước Initiate |
| `otpCode` | ✓ | Mã OTP 6 chữ số nhận qua Email |

**Response `200` — Nội bộ** *(SUCCESS ngay)*

```json
{
  "success": true,
  "message": "Giao dịch đã được thực thi",
  "data": {
    "id": 10,
    "type": "INTERNAL",
    "status": "SUCCESS",
    "amount": 500000.00,
    "fee": 0.00,
    "direction": "DEBIT",
    "counterpartAccount": "9704001928374650",
    "counterpartBank": null,
    "description": "Chuyển tiền ăn trưa",
    "referenceCode": "TXN2025011512345678",
    "createdAt": "2025-01-15T12:00:00"
  }
}
```

**Response `200` — Liên ngân hàng** *(PENDING, kết quả sau 3–8s)*

```json
{
  "success": true,
  "message": "Giao dịch đã được thực thi",
  "data": {
    "id": 11,
    "type": "INTERBANK",
    "status": "PENDING",
    "amount": 500000.00,
    "fee": 0.00,
    "direction": "DEBIT",
    "counterpartAccount": "0123456789",
    "counterpartBank": "VCB",
    "description": "Chuyển tiền học phí | CK den NGUYEN VAN B - Vietcombank (0123456789)",
    "referenceCode": "TXN2025011587654321",
    "createdAt": "2025-01-15T12:01:00"
  }
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `400` | Mã OTP không hợp lệ hoặc đã hết hạn |
| `400` | Phiên xác thực giao dịch không hợp lệ hoặc đã hết hạn |
| `400` | Số dư tài khoản không đủ *(nếu số dư thay đổi giữa 2 bước)* |
| `403` | Không có quyền thực hiện thao tác này |

> **Lưu ý:** `confirmToken` chỉ dùng được **một lần** và hết hạn sau **5 phút** kể từ khi phát hành. Gọi Initiate lại để lấy token mới.

---

## Ngân hàng được hỗ trợ

| `toBankCode` | Tên ngân hàng |
|-------------|--------------|
| `VCB` | Vietcombank |
| `TCB` | Techcombank |
| `MB` | MB Bank |
| `BIDV` | BIDV |
| `VTB` | VietinBank |
| `ACB` | ACB |
| `VPB` | VPBank |
| `TPB` | TPBank |
| `STB` | Sacombank |
| `MSB` | MSB |
| `SHB` | SHB |
| `OCB` | OCB |

---

## Luồng xử lý liên ngân hàng (sau Confirm)

```
Client          Server (confirm thread)       Background thread
  │                      │                           │
  ├─POST /confirm────────▶│                           │
  │               verify OTP                         │
  │               load TransferSession               │
  │               lock fromAccount                   │
  │               deduct balance                     │
  │               save tx PENDING                    │
  │               dispatch() ─────────────────────────▶ sleep 3-8s
  │◀─ 200 PENDING ────────│                           │ simulate outcome
  │                       │                    settle() @Transactional
  │                       │                      update tx SUCCESS/FAILED
  │                       │                      refund if FAILED
  │                       │                      save Notification
  │
  │ (3-8s sau)
  │ GET /notifications ───▶
  │◀─ thông báo kết quả
```

---

## Ghi chú kỹ thuật

### Reference code
Định dạng: `TXN` + `yyyyMMdd` + 8 chữ số ngẫu nhiên  
Ví dụ: `TXN2025011512345678` — duy nhất toàn hệ thống, retry 5 lần nếu trùng.

### Concurrency (internal transfer)

| Lớp | Cơ chế | Vai trò |
|-----|--------|---------|
| Pessimistic lock | `SELECT ... FOR UPDATE` | Serialize transfer cùng lúc trên cùng account |
| Lock ordering | Lock theo ID tăng dần | Tránh deadlock giữa 2 transfer ngược chiều |
| Optimistic lock | `@Version` trên `Account` | Backstop nếu code khác cập nhật balance mà không dùng pessimistic lock |

### Tỷ lệ thành công giả lập
Mặc định **85%** — có thể điều chỉnh qua `app.interbank.success-rate` trong `application.yml`.

### Anti-replay
`TransferSession.used = true` được set ngay khi OTP verified thành công — trước khi thực thi giao dịch. Gọi lại `/confirm` với cùng token sẽ nhận lỗi `TRANSFER_SESSION_NOT_FOUND`.
