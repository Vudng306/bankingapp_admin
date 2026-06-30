# Devices API — `/devices`

> Xem quy ước chung tại [README.md](README.md)  
> Tất cả endpoint yêu cầu **🔒 JWT**

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/devices` | POST | Đăng ký / cập nhật thiết bị (upsert) |
| `/devices` | GET | Danh sách thiết bị của user |
| `/devices/{id}` | DELETE | Vô hiệu hóa thiết bị |

---

## Mục đích

API này quản lý thiết bị Android để phục vụ hai mục tiêu:

1. **Push notification (FCM):** Server lưu push token để gửi thông báo theo sự kiện.
2. **Đăng xuất từ xa (Remote Logout):** Vô hiệu hóa thiết bị → JWT gắn với thiết bị đó bị từ chối **ngay lập tức** trên mọi request tiếp theo, không cần chờ JWT hết hạn.

**Cách hoạt động của Remote Logout:**
- Khi login kèm `deviceId`, JWT được nhúng claim `"did": "<deviceId>"`.
- Mỗi request có JWT chứa claim `did`, server kiểm tra `devices.is_active` trong DB.
- `DELETE /devices/{id}` → đặt `is_active = false` → mọi request dùng JWT cũ của thiết bị đó trả `401` ngay.
- JWT không có claim `did` (token cũ / login không gửi deviceId) → không bị ảnh hưởng (backward compatible).

Mỗi khi user đăng nhập trên một thiết bị mới (hoặc FCM token được làm mới), app **bắt buộc phải gọi `POST /devices`** để server có push token mới nhất.  
Khi user đăng xuất, gọi `DELETE /devices/{id}` để server không gửi thông báo đến thiết bị đó nữa.

---

## POST /devices

Đăng ký thiết bị mới hoặc cập nhật push token nếu đã tồn tại.  
Logic upsert dựa trên cặp `(userId, deviceId)` — cùng thiết bị gọi nhiều lần sẽ **cập nhật** thay vì tạo mới.

**Khi nào cần gọi:**
- Sau mỗi lần đăng nhập thành công
- Khi `FirebaseMessaging.getInstance().getToken()` trả về token mới (callback `onNewToken`)

**Request**

```json
{
  "deviceId": "a1b2c3d4e5f6a1b2",
  "deviceName": "Samsung Galaxy S24",
  "pushToken": "fGH5k9LmN2pQ:APA91bH..."
}
```

| Field | Bắt buộc | Rule | Mô tả |
|-------|----------|------|-------|
| `deviceId` | ✓ | Tối đa 255 ký tự | ID duy nhất của thiết bị. Android: `Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)` |
| `deviceName` | — | Tối đa 100 ký tự | Tên thiết bị để hiển thị (ví dụ: model máy) |
| `pushToken` | — | Tối đa 255 ký tự | FCM registration token. Có thể `null` nếu user chưa cấp quyền thông báo |

**Lấy `pushToken` trên Android:**

```kotlin
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        // Gọi POST /devices với token này
    }
}
```

**Response `200`**

```json
{
  "success": true,
  "message": "Thiết bị đã được đăng ký",
  "data": {
    "id": 5,
    "deviceId": "a1b2c3d4e5f6a1b2",
    "deviceName": "Samsung Galaxy S24",
    "pushEnabled": true,
    "biometricEnabled": false,
    "active": true,
    "lastLoginAt": "2025-01-15T12:00:00",
    "createdAt": "2025-01-01T08:00:00"
  }
}
```

**Cấu trúc `data`**

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | Long | ID bản ghi thiết bị (dùng để DELETE) |
| `deviceId` | String | ANDROID_ID của thiết bị |
| `deviceName` | String | Tên thiết bị |
| `pushEnabled` | Boolean | `true` nếu đã có push token hợp lệ |
| `biometricEnabled` | Boolean | Đăng nhập sinh trắc học (hiện luôn `false`) |
| `active` | Boolean | `false` sau khi đăng xuất |
| `lastLoginAt` | DateTime | Lần đăng nhập gần nhất |
| `createdAt` | DateTime | Lần đầu đăng ký |

---

## GET /devices

Danh sách tất cả thiết bị của user (kể cả đã vô hiệu hóa), mới nhất trước.

**Response `200`**

```json
{
  "success": true,
  "data": [
    {
      "id": 5,
      "deviceId": "a1b2c3d4e5f6a1b2",
      "deviceName": "Samsung Galaxy S24",
      "pushEnabled": true,
      "biometricEnabled": false,
      "active": true,
      "lastLoginAt": "2025-01-15T12:00:00",
      "createdAt": "2025-01-01T08:00:00"
    },
    {
      "id": 2,
      "deviceId": "old-device-id",
      "deviceName": "Pixel 7",
      "pushEnabled": false,
      "biometricEnabled": false,
      "active": false,
      "lastLoginAt": "2024-12-01T09:00:00",
      "createdAt": "2024-11-15T10:00:00"
    }
  ]
}
```

---

## DELETE /devices/{id}

Vô hiệu hóa thiết bị — xóa push token, đặt `active = false`.  
Server sẽ **không gửi FCM** đến thiết bị này cho đến khi đăng ký lại.  
JWT của thiết bị đó **bị từ chối ngay lập tức** (401) nếu JWT được phát hành kèm `deviceId`.

**Khi nào cần gọi:**
- Khi user bấm **Đăng xuất** trên thiết bị hiện tại.
- Khi user muốn **đăng xuất từ xa** một thiết bị khác (từ màn hình quản lý thiết bị).

**Luồng remote logout:**

```
User mở màn hình "Thiết bị đã đăng nhập"
  │
  ├── GET /devices → hiển thị danh sách (cả active + inactive)
  │
  └── User bấm "Đăng xuất" trên thiết bị khác
        │
        ├── DELETE /devices/{id}          ← server đặt is_active = false
        │
        └── Mọi request tiếp theo từ thiết bị đó → 401 Unauthorized
            (ngay cả khi JWT còn hiệu lực về thời gian)
```

**Path param**

| Param | Mô tả |
|-------|-------|
| `id` | ID bản ghi thiết bị (lấy từ GET /devices hoặc response của POST /devices) |

**Response `200`**

```json
{
  "success": true,
  "message": "Thiết bị đã được hủy đăng ký"
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `403` | Không có quyền thực hiện thao tác này |
| `404` | Không tìm thấy tài nguyên |

---

## Luồng tích hợp FCM đầy đủ

```
Đăng nhập                              Đăng xuất
────────────────────────────────────   ─────────────────
POST /auth/login → nhận JWT            DELETE /devices/{id}
   │                                      ↓
   ↓                                   Server xóa push token
FirebaseMessaging.getToken()           Thiết bị không nhận FCM nữa
   │
   ↓
POST /devices { deviceId, pushToken }
   │
   ↓
Server lưu token → gửi FCM khi có
sự kiện (chuyển tiền, tiết kiệm, ...)
```

### FCM payload mẫu nhận trên Android

```json
{
  "notification": {
    "title": "Nhận tiền thành công",
    "body": "+500,000 VND từ TK 9704001234. Số dư: 2,500,000 VND. Mã GD: TXN2025011512345678"
  },
  "data": {
    "type": "TRANSACTION"
  }
}
```

### Xử lý token hết hạn

Firebase tự động làm mới token. Implement `FirebaseMessagingService.onNewToken()` để cập nhật:

```kotlin
class MyFirebaseService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // Gọi POST /devices với token mới (nếu user đang đăng nhập)
        if (userIsLoggedIn()) {
            apiService.registerDevice(RegisterDeviceRequest(
                deviceId = getAndroidId(),
                pushToken = token
            ))
        }
    }
}
```

---

## Push notification tự động theo sự kiện

| Sự kiện | Tiêu đề | Ai nhận |
|---------|---------|---------|
| Chuyển khoản nội bộ thành công | "Chuyển tiền thành công" | Người gửi |
| Nhận chuyển khoản | "Nhận tiền thành công" | Người nhận |
| Lệnh chuyển LNH đang xử lý | "Lệnh chuyển tiền đang xử lý" | Người gửi |
| Chuyển LNH thành công | "Chuyển tiền liên ngân hàng thành công" | Người gửi |
| Chuyển LNH thất bại (hoàn tiền) | "Chuyển tiền liên ngân hàng thất bại" | Người gửi |
| Nạp tiền điện thoại thành công | "Nạp tiền điện thoại thành công" | User |
| Mở sổ tiết kiệm | "Mở sổ tiết kiệm thành công" | User |
| Sổ tiết kiệm đáo hạn | "Sổ tiết kiệm đã đáo hạn" | User |
| Tất toán sổ tiết kiệm | "Tất toán sổ tiết kiệm thành công" | User |

Mỗi push đều kèm số dư tài khoản sau giao dịch (trừ thông báo nhắc đáo hạn).
