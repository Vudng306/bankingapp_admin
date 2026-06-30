# Reports API — `/reports`

> Xem quy ước chung tại [README.md](README.md)  
> Tất cả endpoint yêu cầu **🔒 JWT**

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/reports/spending` | GET | Thống kê thu/chi thô (JSON tổng quát) |
| `/reports/chart/bar` | GET | Dữ liệu sẵn cho biểu đồ cột |
| `/reports/chart/pie` | GET | Dữ liệu sẵn cho biểu đồ tròn |

---

## Tham số chung

| Param | Bắt buộc | Mặc định | Mô tả |
|-------|----------|----------|-------|
| `period` | — | `MONTH` | `MONTH` (nhóm theo tháng) \| `WEEK` (nhóm theo tuần ISO) |
| `from` | — | 3 tháng trước | Ngày bắt đầu, định dạng `YYYY-MM-DD` |
| `to` | — | Hôm nay | Ngày kết thúc, định dạng `YYYY-MM-DD` |
| `accountId` | — | `null` (tất cả) | Lọc theo tài khoản cụ thể |

> `period` chỉ áp dụng cho `/reports/spending` và `/reports/chart/bar`.

---

## Định nghĩa Thu / Chi

| Chiều | Điều kiện | Ví dụ |
|-------|-----------|-------|
| **Chi** (EXPENSE) | `from_account` thuộc user, `to_account` KHÔNG thuộc user (hoặc NULL) | Chuyển khoản ra, gửi tiết kiệm, chuyển LNH |
| **Thu** (INCOME) | `to_account` thuộc user, `from_account` KHÔNG thuộc user (hoặc NULL) | Nhận chuyển khoản, tất toán tiết kiệm, nạp tiền |
| **Loại trừ** | Chuyển giữa 2 TK cùng user | Chuyển nội bộ A→B (net = 0, không phải thu/chi thực) |

Chỉ tính giao dịch có `status = SUCCESS`.

---

## Định dạng label theo kỳ

| `period` | Định dạng | Ví dụ | Sắp xếp |
|----------|-----------|-------|---------|
| `MONTH` | `YYYY-MM` | `"2024-01"`, `"2024-12"` | Alphabetical = chronological |
| `WEEK` | `YYYY-W{ww}` | `"2024-W01"`, `"2024-W53"` | ISO week (Thứ Hai đầu tuần) |

---

## GET /reports/spending

Dữ liệu thu/chi tổng quát — dùng để hiển thị số liệu dạng text/table.

**Request mẫu**

```
GET /reports/spending?period=MONTH&from=2024-01-01&to=2024-03-31
GET /reports/spending?period=WEEK&from=2024-01-01&to=2024-01-31&accountId=1
```

**Response `200`**

```json
{
  "success": true,
  "data": {
    "period": "MONTH",
    "from": "2024-01-01",
    "to": "2024-03-31",
    "totalIncome": 15000000.00,
    "totalExpense": 8000000.00,
    "netFlow": 7000000.00,
    "series": [
      {
        "label": "2024-01",
        "income": 5000000.00,
        "expense": 3000000.00,
        "net": 2000000.00,
        "transactionCount": 8
      },
      {
        "label": "2024-02",
        "income": 6000000.00,
        "expense": 2000000.00,
        "net": 4000000.00,
        "transactionCount": 5
      },
      {
        "label": "2024-03",
        "income": 4000000.00,
        "expense": 3000000.00,
        "net": 1000000.00,
        "transactionCount": 6
      }
    ]
  }
}
```

**Cấu trúc `data`**

| Field | Type | Mô tả |
|-------|------|-------|
| `period` | String | `"MONTH"` hoặc `"WEEK"` |
| `from` / `to` | Date | Khoảng thời gian thực tế áp dụng |
| `totalIncome` | Decimal | Tổng thu toàn kỳ |
| `totalExpense` | Decimal | Tổng chi toàn kỳ |
| `netFlow` | Decimal | `totalIncome − totalExpense` |
| `series[].label` | String | Nhãn kỳ (`"2024-01"` hoặc `"2024-W05"`) |
| `series[].income` | Decimal | Thu trong kỳ |
| `series[].expense` | Decimal | Chi trong kỳ |
| `series[].net` | Decimal | `income − expense` |
| `series[].transactionCount` | Long | Số giao dịch |

> Các kỳ **không có giao dịch** sẽ không xuất hiện trong `series` — frontend tự fill 0 nếu cần.

---

## GET /reports/chart/bar

Dữ liệu **sẵn cho biểu đồ cột** — arrays được căn chỉnh index theo `labels`.

**Request mẫu**

```
GET /reports/chart/bar?period=MONTH&from=2024-01-01&to=2024-03-31
```

**Response `200`**

```json
{
  "success": true,
  "data": {
    "period": "MONTH",
    "from": "2024-01-01",
    "to": "2024-03-31",
    "labels": ["2024-01", "2024-02", "2024-03"],
    "series": [
      {
        "name": "income",
        "label": "Thu nhập",
        "color": "#22C55E",
        "data": [5000000.00, 6000000.00, 4000000.00]
      },
      {
        "name": "expense",
        "label": "Chi tiêu",
        "color": "#EF4444",
        "data": [3000000.00, 2000000.00, 3000000.00]
      },
      {
        "name": "net",
        "label": "Dòng tiền ròng",
        "color": "#3B82F6",
        "data": [2000000.00, 4000000.00, 1000000.00]
      }
    ]
  }
}
```

**Cấu trúc `data`**

| Field | Type | Mô tả |
|-------|------|-------|
| `labels` | `String[]` | Nhãn trục X — các phần tử của `series[].data` tương ứng 1-1 theo index |
| `series[].name` | String | Key nội bộ: `"income"` / `"expense"` / `"net"` |
| `series[].label` | String | Tên hiển thị trên legend |
| `series[].color` | String | Màu hex gợi ý |
| `series[].data` | `Decimal[]` | Giá trị — `data[i]` tương ứng `labels[i]` |

**Dùng với MPAndroidChart:**

```kotlin
// Trục X
chart.xAxis.valueFormatter = IndexAxisValueFormatter(response.labels)

// Chuỗi thu nhập (series[0])
val incomeEntries = response.series[0].data.mapIndexed { i, v ->
    BarEntry(i.toFloat(), v.toFloat())
}
val incomeSet = BarDataSet(incomeEntries, response.series[0].label).apply {
    color = Color.parseColor(response.series[0].color)
}

// Ghép dataset (grouped bar)
chart.data = BarData(incomeSet, expenseSet)
chart.groupBars(0f, 0.1f, 0.05f)
```

**Dùng với Recharts (Web / React Native):**

```javascript
// Chuyển đổi sang format [{label, income, expense, net}, ...]
const chartData = response.labels.map((label, i) => ({
    label,
    income:  response.series[0].data[i],
    expense: response.series[1].data[i],
    net:     response.series[2].data[i],
}))

// <BarChart data={chartData}>
//   <Bar dataKey="income"  fill={response.series[0].color} />
//   <Bar dataKey="expense" fill={response.series[1].color} />
// </BarChart>
```

---

## GET /reports/chart/pie

Dữ liệu **sẵn cho biểu đồ tròn** — phân tích chi tiêu / thu nhập theo loại giao dịch.

**Query params bổ sung**

| Param | Bắt buộc | Mặc định | Mô tả |
|-------|----------|----------|-------|
| `direction` | — | `EXPENSE` | `EXPENSE` (phân tích chi) \| `INCOME` (phân tích thu) |

> `period` **không áp dụng** cho endpoint này (không nhóm theo thời gian).

**Request mẫu**

```
GET /reports/chart/pie?direction=EXPENSE&from=2024-01-01&to=2024-03-31
GET /reports/chart/pie?direction=INCOME&from=2024-01-01&to=2024-03-31
```

**Response `200` — EXPENSE**

```json
{
  "success": true,
  "data": {
    "direction": "EXPENSE",
    "from": "2024-01-01",
    "to": "2024-03-31",
    "total": 8000000.00,
    "slices": [
      {
        "txKey": "INTERBANK",
        "label": "Liên ngân hàng",
        "amount": 4000000.00,
        "percent": 50.0,
        "color": "#F59E0B",
        "count": 2
      },
      {
        "txKey": "INTERNAL",
        "label": "Chuyển khoản",
        "amount": 3000000.00,
        "percent": 37.5,
        "color": "#3B82F6",
        "count": 5
      },
      {
        "txKey": "SAVINGS_DEPOSIT",
        "label": "Gửi tiết kiệm",
        "amount": 1000000.00,
        "percent": 12.5,
        "color": "#8B5CF6",
        "count": 1
      }
    ]
  }
}
```

**Cấu trúc `slices[]`**

| Field | Type | Mô tả |
|-------|------|-------|
| `txKey` | String | Loại giao dịch (enum key) |
| `label` | String | Tên hiển thị tiếng Việt |
| `amount` | Decimal | Tổng tiền của loại này |
| `percent` | Double | Phần trăm so với `total` (làm tròn 2 chữ số) |
| `color` | String | Màu hex gợi ý cho lát cắt |
| `count` | Long | Số lượng giao dịch |

**Bảng màu theo loại giao dịch**

| `txKey` | `label` | Màu |
|---------|---------|-----|
| `INTERNAL` | Chuyển khoản | `#3B82F6` |
| `INTERBANK` | Liên ngân hàng | `#F59E0B` |
| `SAVINGS_DEPOSIT` | Gửi tiết kiệm | `#8B5CF6` |
| `SAVINGS_WITHDRAW` | Tất toán tiết kiệm | `#06B6D4` |
| `TOPUP` | Nạp tiền | `#10B981` |

**Dùng với MPAndroidChart:**

```kotlin
val entries = response.slices.map { PieEntry(it.percent.toFloat(), it.label) }
val colors  = response.slices.map { Color.parseColor(it.color) }

val dataSet = PieDataSet(entries, "").apply {
    setColors(colors)
    valueTextSize = 12f
}
pieChart.data = PieData(dataSet)
pieChart.centerText = "Tổng chi\n${formatVnd(response.total)}"
```

**Lỗi chung cho /reports/**

| HTTP | message |
|------|---------|
| `400` | Dữ liệu đầu vào không hợp lệ (`period` / `direction` không đúng; `from` > `to`) |
| `403` | Không có quyền thực hiện thao tác này (`accountId` không thuộc user) |
| `404` | Không tìm thấy tài khoản |
