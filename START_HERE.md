# 🚀 Hướng Dẫn Khởi Động Lại BizFlow

## ✅ Đã Dọn Dẹp Hoàn Tất!

Docker đã được dọn dẹp sạch sẽ. Bạn có thể tắt máy yên tâm.

---

## 🔄 Khi Khởi Động Lại

### Bước 1: Chạy tất cả containers
```bash
cd "/Users/melaniepham/Documents/Viet/HK1 Năm 3/XDHTHDT/27-01-2026/BizFlow"
docker compose up -d --build
```

⏱️ **Thời gian build**: Khoảng 3-5 phút (tùy máy)

### Bước 2: Kiểm tra trạng thái
```bash
docker compose ps
```

Đảm bảo tất cả 18 containers đang chạy:
- ✅ Gateway (8000)
- ✅ Authentication (8086)
- ✅ Sales (8081)
- ✅ Promotion (8082)
- ✅ Catalog (8083)
- ✅ Inventory (8084)
- ✅ Customer (8085)
- ✅ Report (8087)
- ✅ Frontend (3000)
- ✅ AI Service (5000)
- ✅ MySQL (3307)
- ✅ Redis, RabbitMQ, Kafka, Prometheus, Grafana...

### Bước 3: Truy cập ứng dụng
🌐 **Frontend**: http://localhost:3000

---

## 🆕 Các Thay Đổi Đã Được Áp Dụng

### 1. **Tự Động Nâng Hạng Thành Viên** ✨
- **Bronze**: 0-999 điểm/tháng
- **Silver**: 1,000-2,999 điểm/tháng  
- **Gold**: 3,000-8,999 điểm/tháng
- **Platinum**: 9,000-14,999 điểm/tháng
- **Diamond**: 15,000+ điểm/tháng

👉 Khách hàng mua 6.369.000đ = 6,369 điểm → Sẽ lên **Gold** sau khi thanh toán!

### 2. **Thông Tin Khách Hàng Đầy Đủ** 📋
- ✅ Ngày sinh
- ✅ Giới tính (đã thêm field vào database)
- ✅ Địa chỉ
- ✅ CCCD

### 3. **Hạng Lomas** 🏆
- Hiển thị: Bronze, Silver, Gold, Platinum, Diamond (thay vì DONG, BAC...)
- Mã thẻ Lomas: Hiển thị `--`

### 4. **Nút Sửa Thông Tin** ✏️
- Click "Sửa" → Enable form chỉnh sửa
- Click "Lưu thay đổi" → Cập nhật database

---

## 🛠️ Script Tiện Ích

### Dọn dẹp Docker (nếu cần)
```bash
./cleanup-docker.sh
```

### Xem logs
```bash
# Xem logs của một service
docker compose logs -f customer-service

# Xem logs của tất cả
docker compose logs -f
```

### Restart một service
```bash
docker compose restart customer-service
```

---

## 📝 Lưu Ý Quan Trọng

1. **Database đã được reset** - Dữ liệu cũ đã bị xóa
2. **Lần chạy đầu tiên sẽ lâu hơn** - Do phải build lại images
3. **Nếu gặp lỗi build** - Chạy lại: `docker compose up -d --build`

---

## 🐛 Troubleshooting

### Container không start?
```bash
docker compose logs <service-name>
```

### Port bị chiếm?
```bash
lsof -i :<port>
kill -9 <PID>
```

### Build bị lỗi SSL (Gateway)?
- Script đã fix sẵn với retry logic
- Nếu vẫn lỗi: Kiểm tra kết nối internet

---

**Chúc bạn làm việc hiệu quả! 🎉**
