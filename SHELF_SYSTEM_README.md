# CƠ CHẾ KỆ HÀNG (SHELVES SYSTEM)

## TỔNG QUAN

Hệ thống đã được cập nhật với cơ chế kệ hàng mới:
- **Loại bỏ warehouses**: Không còn sử dụng table `warehouses`
- **Kho nhập hàng**: `inventory_stock` - tồn kho tổng
- **Kệ hàng**: `shelves` - sản phẩm để bán
- **Quy tắc**: Nhân viên CHỈ bán từ kệ, không bán trực tiếp từ kho

## LUỒNG NGHIỆP VỤ

```
1. Nhập hàng → inventory_stock
2. OWNER đưa hàng lên kệ → shelves (trừ inventory_stock, cộng shelves)
3. Nhân viên bán hàng → trừ từ shelves
4. OWNER bỏ hàng khỏi kệ → shelves về inventory_stock
```

## CÀI ĐẶT DATABASE

Chạy migration script:
```bash
mysql -u root -p < db/migration_shelves.sql
```

## API ENDPOINTS

### 1. OWNER - Quản lý kệ hàng

#### Đưa hàng lên kệ
```
POST /api/inventory/shelves/move-to-shelf
Role: OWNER
Body: {
  "productId": 1,
  "quantity": 50,
  "note": "Đưa 50 sản phẩm lên kệ"
}
```

#### Bỏ hàng khỏi kệ (trả về kho)
```
POST /api/inventory/shelves/remove-from-shelf
Role: OWNER
Body: {
  "productId": 1,
  "quantity": 10,
  "note": "Trả 10 sản phẩm về kho"
}
```

### 2. Xem kệ hàng

#### Xem tất cả sản phẩm trên kệ
```
GET /api/inventory/shelves
Role: EMPLOYEE, OWNER, ADMIN
Response: [
  {
    "id": 1,
    "productId": 1,
    "productCode": "CC330",
    "productName": "Coca-Cola lon 330ml",
    "categoryId": 1,
    "quantity": 50,
    "alertLevel": "NORMAL",
    "price": 10000.0,
    "unit": "lon"
  }
]
Note: API CHỈ trả về sản phẩm có quantity > 0
```

#### Xem sản phẩm cụ thể trên kệ
```
GET /api/inventory/shelves/{productId}
Role: EMPLOYEE, OWNER, ADMIN
```

#### Xem sản phẩm cảnh báo trên kệ
```
GET /api/inventory/shelves/low-stock?threshold=10
Role: OWNER
Response: Danh sách sản phẩm có quantity < 10
```

### 3. Báo cáo kệ hàng (OWNER)

#### Báo cáo kệ hàng chi tiết
```
GET /api/reports/shelf-stock?threshold=10
Role: OWNER
Response: [
  {
    "productId": 1,
    "productName": "Coca-Cola lon 330ml",
    "productCode": "CC330",
    "categoryId": 1,
    "quantity": 5,
    "alertLevel": "WARNING"
  }
]
```

#### Tổng quan cảnh báo kệ hàng
```
GET /api/reports/shelf-stock/summary?threshold=10
Role: OWNER
Response: {
  "total": 5,
  "danger": 2,
  "warning": 3
}
```

## CẢNH BÁO TRẠNG THÁI

### Alert Levels
- **NORMAL**: quantity >= 10
- **WARNING**: 1 <= quantity < 10
- **DANGER**: quantity = 0

### Quy tắc tự động
- Khi `quantity = 0` → Tự động XÓA bản ghi khỏi `shelves`
- Khi OWNER bỏ hàng khỏi kệ → Chuyển về `inventory_stock`

## CẤU TRÚC FILES MỚI

### InventoryService
```
entity/
  └── Shelf.java                    # Entity kệ hàng
repository/
  └── ShelfRepository.java          # Repository kệ hàng
service/
  └── ShelfService.java             # Logic nghiệp vụ kệ hàng
controller/
  └── ShelfController.java          # API kệ hàng
dto/
  ├── ShelfMoveRequest.java         # DTO request đưa/bỏ hàng
  └── ShelfStockResponse.java       # DTO response trạng thái kệ
```

### ReportService
```
dto/
  └── ShelfReportDTO.java           # DTO báo cáo kệ hàng
integration/
  └── InventoryClient.java          # Thêm method getAllShelfStocks()
service/
  └── ReportService.java            # Thêm getShelfReport(), getShelfStockSummary()
controller/
  └── ReportController.java         # Thêm endpoints báo cáo kệ hàng
```

## THAY ĐỔI QUAN TRỌNG

### 1. Bán hàng
- **TRƯỚC**: Trừ trực tiếp từ `inventory_stock`
- **SAU**: Trừ từ `shelves` (qua ShelfService)

### 2. Quyền hạn
- **EMPLOYEE**: Xem kệ, bán từ kệ
- **OWNER**: Quản lý kệ (đưa lên/bỏ xuống), xem báo cáo kệ

### 3. Logic nghiệp vụ
```java
// InventoryInternalController - applySale()
// THAY ĐỔI: Gọi ShelfService thay vì InventoryService
shelfService.deductFromShelf(productId, quantity, orderId, userId);
```

## KIỂM TRA HỆ THỐNG

### 1. Kiểm tra database
```sql
-- Xem tồn kho
SELECT * FROM inventory_stocks WHERE product_id = 1;

-- Xem kệ hàng
SELECT * FROM shelves WHERE product_id = 1;
```

### 2. Kiểm tra API
```bash
# Đưa hàng lên kệ
curl -X POST http://localhost:8084/api/inventory/shelves/move-to-shelf \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":20}'

# Xem kệ hàng
curl http://localhost:8084/api/inventory/shelves

# Báo cáo kệ hàng
curl http://localhost:8085/api/reports/shelf-stock?threshold=10
```

## LƯU Ý QUAN TRỌNG

1. **KHÔNG CÒN WAREHOUSES**: Đã loại bỏ hoàn toàn logic warehouses
2. **CHỈ BÁN TỪ KỆ**: Frontend phải lấy danh sách từ `/api/inventory/shelves`
3. **OWNER QUYẾT ĐỊNH**: Chỉ OWNER mới đưa hàng lên kệ
4. **TỰ ĐỘNG XÓA**: Khi `quantity = 0` trên kệ → Xóa khỏi shelves (backend + auto-delete)
5. **BÁO CÁO RIÊNG**: Báo cáo kệ hàng KHÁC với báo cáo tồn kho
6. **TRẠNG THÁI "ĐANG BÁN"**: Sản phẩm tự động hiển thị "✅ Đang bán" khi có trong shelves (quantity > 0), "❌ Ngừng bán" khi không có trong shelves. Không còn button toggle status nữa.
7. **LỌC BACKEND**: API `/api/inventory/shelves` CHỈ trả về sản phẩm có `quantity > 0`
8. **CACHE-BUSTING**: Frontend sử dụng timestamp để tránh cache, luôn lấy data mới nhất

## FRONTEND INTEGRATION

### Hiển thị danh sách bán hàng
```javascript
// employee-dashboard.js
// API với cache-busting và lọc quantity > 0
const timestamp = Date.now();
const response = await fetch(`${API_BASE}/inventory/shelves?_t=${timestamp}`, {
    headers: { 
        'Authorization': `Bearer ${token}`,
        'Cache-Control': 'no-cache, no-store, must-revalidate'
    }
});
const shelvesData = await response.json();
// Backend đã lọc quantity > 0, frontend double-check
products = shelvesData.filter(shelf => shelf.quantity > 0);
```

### Module OWNER
```javascript
// Báo cáo kệ hàng
GET /api/reports/shelf-stock?threshold=10

// Đưa hàng lên kệ
POST /api/inventory/shelves/move-to-shelf

// Bỏ hàng khỏi kệ
POST /api/inventory/shelves/remove-from-shelf
```

## TROUBLESHOOTING

### Lỗi: "Product not available on shelf for sale"
**Nguyên nhân**: Sản phẩm chưa có trên kệ
**Giải pháp**: OWNER cần đưa sản phẩm từ kho lên kệ

### Lỗi: "Insufficient stock in inventory"
**Nguyên nhân**: Không đủ hàng trong kho để đưa lên kệ
**Giải pháp**: Nhập thêm hàng vào `inventory_stock`

### Lỗi: "Insufficient quantity on shelf"
**Nguyên nhân**: Không đủ hàng trên kệ để bán
**Giải pháp**: OWNER đưa thêm hàng lên kệ

## HỖ TRỢ

Nếu có thắc mắc về cơ chế kệ hàng, vui lòng liên hệ team phát triển.
