# Feature: Spending Report (Báo cáo chi tiêu)

- **Phần**: 2 (Nâng cao)
- **Mô tả**: Biểu đồ thống kê thu/chi theo tuần, tháng.
- **Entity liên quan**: `Transaction`, `Account`

## Chức năng con
1. **Thống kê theo tuần**: tổng thu, tổng chi theo từng tuần.
2. **Thống kê theo tháng**: tổng thu, tổng chi theo từng tháng.
3. **Dữ liệu biểu đồ**: trả về format sẵn cho frontend vẽ cột/tròn.

## Quy tắc nghiệp vụ
- **Không cần bảng riêng**: tổng hợp trực tiếp từ `transactions`.
- "Chi" = giao dịch mà account của user là `from`; "Thu" = account của user là `to`.
- Nhóm theo thời gian bằng `GROUP BY` (tuần/tháng) trên `created_at`.
- Chỉ tính giao dịch `status = 'success'`.
- Có thể nhóm thêm theo `type` để phân loại chi tiêu.

## Schema dữ liệu sử dụng
- Đọc `transactions` (aggregate SUM(amount), GROUP BY thời gian, lọc theo account của user).

## Gợi ý endpoint
- `GET /reports/spending?period=weekly`
- `GET /reports/spending?period=monthly`
