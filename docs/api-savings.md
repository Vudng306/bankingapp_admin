# Savings API — `/savings`

> Xem quy ước chung tại [README.md](README.md)  
> Tất cả endpoint yêu cầu **🔒 JWT**

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/savings` | POST | Mở sổ tiết kiệm |
| `/savings` | GET | Danh sách sổ tiết kiệm |
| `/savings/{id}` | GET | Chi tiết một sổ |
| `/savings/{id}/withdraw` | POST | Tất toán sổ |

---

## Quy tắc nghiệp vụ

### Kỳ hạn và lãi suất

| `termMonths` | Lãi suất (%/năm) |
|-------------|-----------------|
| `1` | 3.50% |
| `3` | 4.50% |
| `6` | 5.50% |
| `12` | 6.50% |
| `24` | 7.00% |

Chỉ chấp nhận **đúng 5 giá trị trên** — các kỳ hạn khác trả lỗi `400`.

### Số tiền tối thiểu
**1,000,000 VND** (cấu hình qua `app.savings.min-amount`).

### Công thức lãi

| Trường hợp | Công thức |
|-----------|----------|
| Đúng/quá hạn | `I = P × (r/100) × termMonths/12` |
| Rút sớm | `I = P × (0.50/100) × actualDays/365` |

Lãi suất không kỳ hạn (rút sớm) cố định **0.50%/năm**.

### Trạng thái sổ (`status`)

| Giá trị | Ý nghĩa |
|---------|---------|
| `ACTIVE` | Đang chạy, chưa đến hạn |
| `MATURED` | Đã đến ngày đáo hạn, chưa tất toán |
| `WITHDRAWN` | Đã tất toán |

---

## POST /savings

Mở sổ tiết kiệm — trừ tiền từ tài khoản nguồn ngay lập tức.

**Request**

```json
{
  "fromAccountId": 1,
  "amount": 5000000.00,
  "termMonths": 6,
  "pin": "123456"
}
```

| Field | Bắt buộc | Rule |
|-------|----------|------|
| `fromAccountId` | ✓ | ID tài khoản nguồn (phải thuộc user đang đăng nhập, đang ACTIVE) |
| `amount` | ✓ | Tối thiểu **1,000,000 VND**, không vượt quá số dư hiện tại |
| `termMonths` | ✓ | Một trong: `1`, `3`, `6`, `12`, `24` |
| `pin` | ✓ | PIN giao dịch 6 chữ số |

**Response `200`**

```json
{
  "success": true,
  "message": "Mở sổ tiết kiệm thành công",
  "data": {
    "id": 3,
    "sourceAccountNumber": "9704081928374650",
    "principal": 5000000.00,
    "interestRate": 5.50,
    "termMonths": 6,
    "startDate": "2025-01-15",
    "maturityDate": "2025-07-15",
    "accruedInterest": 0.00,
    "expectedInterest": 137500.00,
    "expectedPayout": 5137500.00,
    "earlyWithdrawInterest": 0.00,
    "earlyWithdrawPayout": 5000000.00,
    "status": "ACTIVE",
    "matured": false,
    "daysRemaining": 181,
    "createdAt": "2025-01-15T09:00:00"
  }
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `400` | Chưa thiết lập mã PIN giao dịch |
| `400` | Mã PIN không đúng |
| `400` | Số dư tài khoản không đủ |
| `400` | Số tiền gửi tiết kiệm tối thiểu là 1,000,000 VND |
| `400` | Kỳ hạn không hợp lệ. Hỗ trợ: 1, 3, 6, 12, 24 tháng |
| `403` | Không có quyền / tài khoản bị khóa |
| `404` | Không tìm thấy tài khoản |

---

## GET /savings

Danh sách tất cả sổ tiết kiệm của user (mọi trạng thái, mới nhất trước).

**Response `200`**

```json
{
  "success": true,
  "message": "Danh sách sổ tiết kiệm",
  "data": [ /* mảng SavingsResponse, cùng cấu trúc như POST /savings */ ]
}
```

---

## GET /savings/{id}

Chi tiết một sổ tiết kiệm. Các trường realtime (`earlyWithdrawInterest`, `daysRemaining`) được tính lại mỗi lần gọi.

**Response `200`** — cùng cấu trúc `SavingsResponse` như trên.

**Lỗi**

| HTTP | message |
|------|---------|
| `404` | Không tìm thấy sổ tiết kiệm |

---

## POST /savings/{id}/withdraw

Tất toán sổ — trả tiền về tài khoản nguồn, đóng sổ.

**Request**

```json
{
  "pin": "123456"
}
```

**Response `200` — Tất toán đúng hạn**

```json
{
  "success": true,
  "message": "Tất toán thành công",
  "data": {
    "savingsId": 3,
    "accountNumber": "9704081928374650",
    "principal": 5000000.00,
    "interestEarned": 137500.00,
    "totalPayout": 5137500.00,
    "earlyWithdrawal": false,
    "actualDaysHeld": null,
    "referenceCode": "WDR2025071512345678"
  }
}
```

**Response `200` — Tất toán sớm** *(trước ngày đáo hạn)*

```json
{
  "success": true,
  "message": "Tất toán thành công",
  "data": {
    "savingsId": 3,
    "accountNumber": "9704081928374650",
    "principal": 5000000.00,
    "interestEarned": 2054.79,
    "totalPayout": 5002054.79,
    "earlyWithdrawal": true,
    "actualDaysHeld": 30,
    "referenceCode": "WDR2025021512345678"
  }
}
```

**Cấu trúc `data`**

| Field | Type | Mô tả |
|-------|------|-------|
| `savingsId` | Long | ID sổ tiết kiệm |
| `accountNumber` | String | Số tài khoản nhận tiền |
| `principal` | Decimal | Số tiền gốc |
| `interestEarned` | Decimal | Lãi thực nhận |
| `totalPayout` | Decimal | Tổng hoàn về = gốc + lãi |
| `earlyWithdrawal` | Boolean | `true` = rút sớm, mất một phần lãi |
| `actualDaysHeld` | Long | Số ngày thực gửi (`null` nếu đúng/quá hạn) |
| `referenceCode` | String | Mã tham chiếu giao dịch |

**Lỗi**

| HTTP | message |
|------|---------|
| `400` | Mã PIN không đúng |
| `400` | Sổ tiết kiệm đã đóng |
| `404` | Không tìm thấy sổ tiết kiệm |

---

## Cấu trúc đầy đủ `SavingsResponse`

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | Long | ID sổ |
| `sourceAccountNumber` | String | Số TK nguồn (TK đã bị trừ khi mở, sẽ nhận khi tất toán) |
| `principal` | Decimal | Số tiền gốc |
| `interestRate` | Decimal | Lãi suất năm (ví dụ: `5.50`) |
| `termMonths` | Integer | Kỳ hạn (tháng) |
| `startDate` | Date | Ngày mở sổ |
| `maturityDate` | Date | Ngày đáo hạn |
| `accruedInterest` | Decimal | Lãi tích lũy thực tế đến hôm nay (cập nhật bởi job hàng đêm — hiển thị "lãi đang chạy") |
| `expectedInterest` | Decimal | Lãi dự kiến khi đủ kỳ hạn (không đổi — theo "hợp đồng") |
| `expectedPayout` | Decimal | `principal + expectedInterest` |
| `earlyWithdrawInterest` | Decimal | Lãi nếu rút **hôm nay** (lãi suất không kỳ hạn × ngày thực gửi) |
| `earlyWithdrawPayout` | Decimal | `principal + earlyWithdrawInterest` |
| `status` | String | `ACTIVE` / `MATURED` / `WITHDRAWN` |
| `matured` | Boolean | `true` nếu đã đến hoặc qua ngày đáo hạn |
| `daysRemaining` | Long | Số ngày còn lại (0 nếu đã đáo hạn hoặc đã tất toán) |
| `createdAt` | DateTime | Thời gian tạo |

---

## Gợi ý UX

| Trạng thái | Hiển thị gợi ý |
|-----------|---------------|
| `ACTIVE`, `matured=false` | Hiện `accruedInterest` ("Lãi đang chạy: x VND") + `daysRemaining` ngày còn lại |
| `ACTIVE`, `matured=true` | Hiện banner "Sổ đã đáo hạn — tất toán ngay để nhận `expectedPayout`" |
| `MATURED` | Hiện nút Tất toán, nhấn mạnh `expectedPayout` |
| `WITHDRAWN` | Hiện lịch sử, không có nút Tất toán |

Khi user xem detail trước khi tất toán:
- Hiển thị **hai kịch bản** song song:
  - *Rút hôm nay:* `earlyWithdrawPayout` (nếu `matured=false`)
  - *Đợi đến đáo hạn:* `expectedPayout`

---

## Push notification tự động

| Sự kiện | Tiêu đề push | Khi nào |
|---------|-------------|---------|
| Mở sổ | "Mở sổ tiết kiệm thành công" | Ngay khi POST /savings |
| Đáo hạn | "Sổ tiết kiệm đã đáo hạn" | Job 01:05 hàng ngày |
| Tất toán | "Tất toán sổ tiết kiệm thành công" | Ngay khi POST /savings/{id}/withdraw |
