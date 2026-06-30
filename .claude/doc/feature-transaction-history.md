# Feature: Transaction History (Lịch sử giao dịch)

- **Phần**: 1 (Core)
- **Mô tả**: Xem, lọc, tìm kiếm lịch sử giao dịch và xuất file PDF/Excel.
- **Entity liên quan**: `Transaction`, `Account`

## Chức năng con
1. **Danh sách + phân trang**: tất cả giao dịch của user, mới nhất trước.
2. **Lọc**: theo khoảng ngày, theo `type`.
3. **Tìm kiếm**: theo nội dung (`description`) hoặc `reference_code`.
4. **Xuất file**: PDF (OpenPDF/iText) và/hoặc Excel (Apache POI).

## Quy tắc nghiệp vụ
- Chỉ trả giao dịch liên quan tới account của user (from hoặc to).
- Lọc động: kết hợp nhiều điều kiện (ngày + loại + từ khóa) bằng JPA Specification hoặc query động.
- File xuất phản ánh đúng bộ lọc đang áp dụng.
- Phân trang để tránh tải quá nhiều bản ghi.

## Schema dữ liệu sử dụng
- Đọc `transactions` (lọc theo account, `created_at`, `type`, `description`, `reference_code`).

## Gợi ý endpoint
- `GET /transactions`            (query params: fromDate, toDate, type, keyword, page, size)
- `GET /transactions/export/pdf`
- `GET /transactions/export/excel`
