# Profile API — `/profile`

> Xem quy ước chung tại [README.md](README.md)  
> Tất cả endpoint yêu cầu **🔒 JWT**

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/profile` | GET | Lấy thông tin hồ sơ + danh sách tài khoản |
| `/profile/password` | PUT | Đổi mật khẩu |
| `/profile/pin` | PUT | Thiết lập / đổi PIN giao dịch |
| `/profile/avatar` | POST | Upload ảnh đại diện |

---

## GET /profile

**Response `200`**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "fullName": "Nguyễn Văn A",
    "email": "user@example.com",
    "phone": "0901234567",
    "avatarUrl": "/uploads/avatars/1_1720000000000.jpg",
    "status": "ACTIVE",
    "createdAt": "2025-01-15T10:30:00",
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
    ]
  }
}
```

> `avatarUrl` là đường dẫn tương đối. Ảnh được serve tại `{BASE_URL}{avatarUrl}`.

---

## PUT /profile/password

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

## PUT /profile/pin

Xử lý cả **thiết lập lần đầu** và **đổi PIN** trong một endpoint duy nhất.

**Lần đầu thiết lập** (chưa có PIN — bỏ qua `currentPin`):

```json
{
  "newPin": "123456"
}
```

**Đổi PIN** (đã có PIN — bắt buộc có `currentPin`):

```json
{
  "currentPin": "123456",
  "newPin": "654321"
}
```

| Field | Rule |
|-------|------|
| `currentPin` | Bắt buộc nếu đã thiết lập PIN trước đó |
| `newPin` | Bắt buộc, đúng 6 chữ số |

**Response `200`**

```json
{
  "success": true,
  "message": "Cập nhật PIN thành công"
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `400` | Mã PIN không đúng |
| `400` | PIN phải là 6 chữ số |

---

## POST /profile/avatar

```
Content-Type: multipart/form-data
```

**Form field**

| Field | Yêu cầu |
|-------|---------|
| `file` | JPEG hoặc PNG, tối đa **2MB** |

**Response `200`**

```json
{
  "success": true,
  "message": "Cập nhật ảnh đại diện thành công",
  "data": {
    "avatarUrl": "/uploads/avatars/1_1720000000000.jpg"
  }
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `400` | Chỉ chấp nhận file JPEG hoặc PNG |
| `400` | Kích thước file không được vượt quá 2MB |
