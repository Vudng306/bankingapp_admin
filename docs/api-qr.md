# QR Code API — `/qr`

> Xem quy ước chung tại [README.md](README.md)  
> Tất cả endpoint yêu cầu **🔒 JWT**

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/qr/generate` | GET | Sinh mã QR VietQR cho tài khoản |

---

## GET /qr/generate

Sinh mã QR **VietQR** cho tài khoản của user đang đăng nhập.

**Backend logic:**
- Nếu server được cấu hình `VIETQR_CLIENT_ID` + `VIETQR_API_KEY` → gọi `POST https://api.vietqr.io/v2/generate`, trả về `qrContent` (chuỗi EMVCo) + `qrDataURL` (ảnh PNG base64 có logo ngân hàng).
- Nếu chưa cấu hình (hoặc API lỗi) → tự sinh QR bằng EMVCo, chỉ trả về `qrContent`, không có `qrDataURL`.

**Query params**

| Param | Bắt buộc | Rule | Mô tả |
|-------|----------|------|-------|
| `accountId` | ✓ | Phải thuộc user đang đăng nhập | Tài khoản nhận tiền |
| `amount` | — | Tối thiểu **1,000 VND** nếu có | Số tiền cố định (để trống → người chuyển tự nhập) |
| `description` | — | Tối đa **25 ký tự** | Nội dung chuyển tiền |

**Request mẫu**

```
GET /qr/generate?accountId=1
GET /qr/generate?accountId=1&amount=150000&description=Tien+com
```

**Response `200` — khi VietQR API được bật**

```json
{
  "success": true,
  "message": "Tạo mã QR thành công",
  "data": {
    "qrContent": "00020101021238570010A00000072701270006970488...",
    "qrDataURL": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
    "accountNumber": "9704081928374650",
    "accountName": "Nguyễn Văn A",
    "bankBin": "970488",
    "bankName": "Nhom8 Bank",
    "amount": 150000.00,
    "description": "Tien com"
  }
}
```

**Response `200` — khi chạy fallback EMVCo (chưa cấu hình API key)**

```json
{
  "success": true,
  "message": "Tạo mã QR thành công",
  "data": {
    "qrContent": "00020101021238570010A00000072701270006970488...",
    "accountNumber": "9704081928374650",
    "accountName": "Nguyễn Văn A",
    "bankBin": "970488",
    "bankName": "Nhom8 Bank",
    "amount": 150000.00,
    "description": "Tien com"
  }
}
```

> `qrDataURL` **không có** khi chạy fallback — field bị bỏ qua trong JSON (`@JsonInclude(NON_NULL)`).

**Cấu trúc `data`**

| Field | Type | Mô tả |
|-------|------|-------|
| `qrContent` | String | Chuỗi EMVCo đầy đủ — dùng để tự render QR bằng ZXing (luôn có) |
| `qrDataURL` | String | Data URI ảnh PNG base64 có logo ngân hàng (chỉ có khi VietQR API bật) |
| `accountNumber` | String | Số tài khoản nhận |
| `accountName` | String | Tên chủ tài khoản |
| `bankBin` | String | BIN ngân hàng (6 chữ số) |
| `bankName` | String | Tên ngân hàng |
| `amount` | Decimal | `null` nếu không gắn số tiền cố định |
| `description` | String | `null` nếu không có nội dung |

**Lỗi**

| HTTP | message |
|------|---------|
| `403` | Không có quyền thực hiện thao tác này |
| `404` | Không tìm thấy tài khoản |

---

## Hướng dẫn render QR trên Android

### Ưu tiên: dùng `qrDataURL` (nếu có)

Khi VietQR API được bật, `qrDataURL` là ảnh PNG base64 đã có logo ngân hàng — hiển thị trực tiếp:

```kotlin
val dataUrl = response.data.qrDataURL
if (dataUrl != null) {
    // Bỏ tiền tố "data:image/png;base64,"
    val base64 = dataUrl.substringAfter("base64,")
    val bytes  = Base64.decode(base64, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    imageViewQr.setImageBitmap(bitmap)
}
```

### Fallback: render `qrContent` bằng ZXing

Khi `qrDataURL` là null (chưa cấu hình API key hoặc API lỗi):

```kotlin
// build.gradle
implementation 'com.google.zxing:core:3.5.2'
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'

// Code
val encoder = BarcodeEncoder()
val bitmap = encoder.encodeBitmap(
    response.data.qrContent,
    BarcodeFormat.QR_CODE,
    512, 512
)
imageViewQr.setImageBitmap(bitmap)
```

### Gợi ý: tự động chọn nguồn

```kotlin
fun displayQr(data: QrResponse) {
    val qrDataURL = data.qrDataURL
    if (qrDataURL != null) {
        val base64 = qrDataURL.substringAfter("base64,")
        val bytes  = Base64.decode(base64, Base64.DEFAULT)
        imageViewQr.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
    } else {
        val bitmap = BarcodeEncoder().encodeBitmap(
            data.qrContent, BarcodeFormat.QR_CODE, 512, 512
        )
        imageViewQr.setImageBitmap(bitmap)
    }
}
```

---

## Cấu hình server (cho devops)

Để bật VietQR API, set 2 biến môi trường khi chạy server (lấy credentials tại https://my.vietqr.io):

```bash
VIETQR_CLIENT_ID=your-client-id
VIETQR_API_KEY=your-api-key
```

Khi chưa set → server tự sinh QR bằng EMVCo (hoạt động offline, không phụ thuộc bên ngoài).

Template mặc định: `compact` (540×540, có logo Napas + ngân hàng).  
Có thể override: `compact2` (540×640), `qr_only` (480×480), `print` (600×776).

---

## Chuẩn VietQR / EMVCo

Chuỗi `qrContent` tuân theo cấu trúc **TLV** (Tag-Length-Value):

| Tag | Nội dung |
|-----|---------|
| `00` | Payload Format Indicator (`01`) |
| `01` | Point of Initiation Method (`11` = static, `12` = dynamic có amount) |
| `38` | Merchant Account — chứa GUID Napas + BIN + số tài khoản |
| `52` | Merchant Category Code (`0000`) |
| `53` | Transaction Currency (`704` = VND) |
| `54` | Transaction Amount — **chỉ có khi `amount` được truyền** |
| `58` | Country Code (`VN`) |
| `59` | Merchant Name |
| `60` | Merchant City |
| `62` | Additional Data — chứa nội dung chuyển khoản (Tag `08`) |
| `63` | CRC-16/CCITT-FALSE (4 ký tự hex) |

QR này tương thích với tất cả app ngân hàng hỗ trợ VietQR (MB Bank, Vietcombank, Techcombank, BIDV, ...).
