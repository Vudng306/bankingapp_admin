# Dashboard API — `/dashboard`

> Xem quy ước chung tại [README.md](README.md)  
> Tất cả endpoint yêu cầu **🔒 JWT**

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/dashboard/summary` | GET | Tóm tắt toàn bộ màn hình home |
| `/dashboard/transactions/recent` | GET | Giao dịch gần đây |

---

## GET /dashboard/summary

Trả về một lần: danh sách tài khoản + tổng số dư + 5 giao dịch gần nhất + số thông báo chưa đọc.  
Dùng cho **màn hình home** — chỉ cần 1 request duy nhất.

**Response `200`**

```json
{
  "success": true,
  "data": {
    "accounts": [
      {
        "id": 1,
        "accountNumber": "9704001847362910",
        "balance": 50000000.00,
        "currency": "VND",
        "accountType": "PAYMENT",
        "status": "ACTIVE",
        "createdAt": "2025-01-15T10:30:00"
      }
    ],
    "totalBalance": 50000000.00,
    "recentTransactions": [
      {
        "id": 3,
        "type": "INTERNAL",
        "status": "SUCCESS",
        "amount": 500000.00,
        "fee": 0.00,
        "direction": "DEBIT",
        "counterpartAccount": "9704001928374650",
        "counterpartBank": null,
        "description": "Chuyển tiền ăn trưa",
        "referenceCode": "TXN20250115001",
        "createdAt": "2025-01-15T12:00:00"
      }
    ],
    "unreadNotificationCount": 2
  }
}
```

**Cấu trúc `accounts[]`**

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | Long | ID tài khoản |
| `accountNumber` | String | Số tài khoản 16 chữ số |
| `balance` | Decimal | Số dư hiện tại (VND) |
| `currency` | String | Đơn vị tiền tệ (mặc định `VND`) |
| `accountType` | String | `PAYMENT` / `SAVING` |
| `status` | String | `ACTIVE` / `LOCKED` |

**Cấu trúc `recentTransactions[]`**

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | Long | ID giao dịch |
| `type` | String | `INTERNAL`, `INTERBANK`, `TOPUP`, `SAVINGS_DEPOSIT`, `SAVINGS_WITHDRAW` |
| `status` | String | `PENDING`, `SUCCESS`, `FAILED` |
| `amount` | Decimal | Số tiền giao dịch |
| `fee` | Decimal | Phí giao dịch |
| `direction` | String | `DEBIT` (tiền ra) / `CREDIT` (tiền vào) |
| `counterpartAccount` | String | Số tài khoản đối ứng |
| `counterpartBank` | String | Mã ngân hàng (chỉ có với `INTERBANK`, null nếu nội bộ) |
| `description` | String | Nội dung giao dịch |
| `referenceCode` | String | Mã tham chiếu duy nhất |
| `createdAt` | DateTime | Thời gian tạo |

---

## GET /dashboard/transactions/recent

Lấy danh sách giao dịch gần đây, có thể tuỳ chỉnh số lượng.

**Query params**

| Param | Mặc định | Tối đa | Mô tả |
|-------|----------|--------|-------|
| `limit` | `10` | `50` | Số lượng giao dịch trả về |

**Ví dụ**

```
GET /dashboard/transactions/recent?limit=20
```

**Response `200`**

```json
{
  "success": true,
  "data": [
    {
      "id": 3,
      "type": "INTERNAL",
      "status": "SUCCESS",
      "amount": 500000.00,
      "fee": 0.00,
      "direction": "DEBIT",
      "counterpartAccount": "9704001928374650",
      "counterpartBank": null,
      "description": "Chuyển tiền ăn trưa",
      "referenceCode": "TXN20250115001",
      "createdAt": "2025-01-15T12:00:00"
    }
  ]
}
```

> Cấu trúc từng phần tử giống `recentTransactions[]` trong `/dashboard/summary`.
