# Transactions API — `/transactions`

> Xem quy ước chung tại [README.md](README.md)  
> Tất cả endpoint yêu cầu **🔒 JWT**

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/transactions/{id}/receipt` | GET | Biên lai chi tiết của một giao dịch |
| `/transactions/history` | GET | Lịch sử giao dịch của một tài khoản (phân trang) |

---

## GET /transactions/{id}/receipt

Trả về biên lai đầy đủ — chỉ truy cập được nếu user là bên gửi hoặc bên nhận.

**Path param**

| Param | Mô tả |
|-------|-------|
| `id` | ID giao dịch |

**Response `200` — Chuyển khoản nội bộ**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "referenceCode": "TXN2025011512345678",
    "type": "INTERNAL",
    "status": "SUCCESS",
    "fromAccountNumber": "9704001847362910",
    "fromAccountName": "Nguyễn Văn A",
    "toAccountNumber": "9704001928374650",
    "toAccountName": "Trần Thị B",
    "toBankCode": null,
    "toBankName": null,
    "amount": 500000.00,
    "fee": 0.00,
    "totalDeducted": 500000.00,
    "description": "Chuyển tiền ăn trưa",
    "createdAt": "2025-01-15T12:00:00"
  }
}
```

**Response `200` — Chuyển liên ngân hàng**

```json
{
  "success": true,
  "data": {
    "id": 11,
    "referenceCode": "TXN2025011587654321",
    "type": "INTERBANK",
    "status": "SUCCESS",
    "fromAccountNumber": "9704001847362910",
    "fromAccountName": "Nguyễn Văn A",
    "toAccountNumber": "0123456789",
    "toAccountName": "NGUYEN VAN B",
    "toBankCode": "VCB",
    "toBankName": "Vietcombank",
    "amount": 500000.00,
    "fee": 0.00,
    "totalDeducted": 500000.00,
    "description": "Chuyển tiền học phí | CK den NGUYEN VAN B - Vietcombank (0123456789)",
    "createdAt": "2025-01-15T12:01:00"
  }
}
```

> Các field `null` bị ẩn trong response nhờ `@JsonInclude(NON_NULL)`.

**Lỗi**

| HTTP | message |
|------|---------|
| `404` | Không tìm thấy giao dịch |
| `403` | Không có quyền thực hiện thao tác này |

---

## GET /transactions/history

Lịch sử giao dịch của một tài khoản — bao gồm cả gửi và nhận, mới nhất trước.  
Hỗ trợ lọc theo loại, trạng thái, khoảng ngày, và tìm kiếm full-text.

**Query params**

| Param | Bắt buộc | Mặc định | Tối đa | Mô tả |
|-------|----------|----------|--------|-------|
| `accountId` | ✓ | — | — | ID tài khoản (phải thuộc user đang đăng nhập) |
| `type` | — | — | — | Lọc loại: `INTERNAL` `INTERBANK` `TOPUP` `SAVINGS_DEPOSIT` `SAVINGS_WITHDRAW` |
| `status` | — | — | — | Lọc trạng thái: `PENDING` `SUCCESS` `FAILED` |
| `fromDate` | — | — | — | Từ ngày (inclusive), định dạng `yyyy-MM-dd` |
| `toDate` | — | — | — | Đến ngày (inclusive), định dạng `yyyy-MM-dd` |
| `keyword` | — | — | — | Tìm trong `description` hoặc `referenceCode` (không phân biệt hoa/thường) |
| `page` | — | `0` | — | Trang (bắt đầu từ 0) |
| `size` | — | `20` | `50` | Số bản ghi mỗi trang |

**Ví dụ**

```
# Không filter
GET /transactions/history?accountId=1&page=0&size=20

# Chỉ chuyển nội bộ thành công trong tháng 6/2026
GET /transactions/history?accountId=1&type=INTERNAL&status=SUCCESS&fromDate=2026-06-01&toDate=2026-06-30

# Tìm theo mã giao dịch hoặc nội dung chuyển khoản
GET /transactions/history?accountId=1&keyword=TXN20260628&size=5

# Kết hợp nhiều filter
GET /transactions/history?accountId=1&type=INTERBANK&fromDate=2026-01-01&keyword=học phí
```

**Response `200`**

```json
{
  "success": true,
  "data": {
    "content": [
      {
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
      },
      {
        "id": 9,
        "type": "INTERNAL",
        "status": "SUCCESS",
        "amount": 200000.00,
        "fee": 0.00,
        "direction": "CREDIT",
        "counterpartAccount": "9704001928374650",
        "counterpartBank": null,
        "description": null,
        "referenceCode": "TXN2025011512345677",
        "createdAt": "2025-01-14T09:00:00"
      }
    ],
    "totalElements": 10,
    "totalPages": 1,
    "number": 0,
    "size": 20,
    "first": true,
    "last": true
  }
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `404` | Không tìm thấy tài khoản |
| `403` | Không có quyền thực hiện thao tác này |

---

## Thông báo tự động sau giao dịch

Sau mỗi giao dịch thành công, hệ thống tự động tạo thông báo trong `GET /notifications`:

| Loại giao dịch | Ai nhận | Nội dung |
|---------------|---------|---------|
| Nội bộ (SUCCESS) | Bên gửi | "Bạn đã chuyển X VND đến TK Y. Mã GD: Z" |
| Nội bộ (SUCCESS) | Bên nhận | "Tài khoản của bạn vừa nhận X VND từ TK A. Mã GD: Z" |
| Liên ngân hàng (submit) | Bên gửi | "Đang xử lý chuyển X VND đến TK Y (Tên) tại Ngân hàng…" |
| Liên ngân hàng (SUCCESS) | Bên gửi | "Đã chuyển X VND đến TK Y (Tên) tại Ngân hàng…" |
| Liên ngân hàng (FAILED) | Bên gửi | "Giao dịch Z thất bại. X VND đã được hoàn lại…" |
