# Notifications API — `/notifications`

> Xem quy ước chung tại [README.md](README.md)  
> Tất cả endpoint yêu cầu **🔒 JWT**

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/notifications` | GET | Danh sách thông báo (phân trang) |
| `/notifications/unread-count` | GET | Số thông báo chưa đọc |
| `/notifications/{id}/read` | PATCH | Đánh dấu một thông báo đã đọc |
| `/notifications/read-all` | PATCH | Đánh dấu tất cả đã đọc |

---

## GET /notifications

**Query params**

| Param | Mặc định | Tối đa | Mô tả |
|-------|----------|--------|-------|
| `page` | `0` | — | Trang (bắt đầu từ 0) |
| `size` | `20` | `50` | Số bản ghi mỗi trang |

**Response `200`**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Biến động số dư",
        "content": "Tài khoản của bạn vừa nhận 500,000 VND từ 9704001928374650",
        "type": "TRANSACTION",
        "read": false,
        "createdAt": "2025-01-15T12:00:00"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "number": 0,
    "size": 20,
    "first": true,
    "last": true
  }
}
```

**Cấu trúc `content[]`**

| Field | Type | Mô tả |
|-------|------|-------|
| `id` | Long | ID thông báo |
| `title` | String | Tiêu đề |
| `content` | String | Nội dung chi tiết |
| `type` | String | `TRANSACTION` / `BALANCE` / `SYSTEM` |
| `read` | Boolean | `false` = chưa đọc |
| `createdAt` | DateTime | Thời gian tạo |

**Pagination metadata**

| Field | Mô tả |
|-------|-------|
| `totalElements` | Tổng số thông báo |
| `totalPages` | Tổng số trang |
| `number` | Trang hiện tại (bắt đầu từ 0) |
| `first` / `last` | Có phải trang đầu / cuối không |

---

## GET /notifications/unread-count

Lấy số thông báo chưa đọc — dùng để hiển thị **badge**.

**Response `200`**

```json
{
  "success": true,
  "data": {
    "count": 2
  }
}
```

> Gọi endpoint này khi app được focus lại để cập nhật badge.

---

## PATCH /notifications/{id}/read

Đánh dấu một thông báo cụ thể là đã đọc.

**Path param**

| Param | Mô tả |
|-------|-------|
| `id` | ID thông báo |

**Response `200`**

```json
{
  "success": true,
  "message": "Đã đánh dấu đã đọc"
}
```

**Lỗi**

| HTTP | message |
|------|---------|
| `404` | Không tìm thấy tài nguyên |
| `403` | Không có quyền thực hiện thao tác này |

---

## PATCH /notifications/read-all

Đánh dấu **tất cả** thông báo của người dùng là đã đọc.

**Response `200`**

```json
{
  "success": true,
  "message": "Đã đánh dấu tất cả đã đọc"
}
```
