# THÔNG TIN ĐĂNG NHẬP BIZFLOW

## ✅ Đã Sửa và Kiểm Tra - Ngày 29/01/2026

### 🔐 Tài Khoản Mặc Định

Tất cả các tài khoản đều sử dụng mật khẩu: **`admin123`**

| Tên đăng nhập | Mật khẩu | Vai trò | Mô tả |
|--------------|----------|---------|-------|
| `admin` | `admin123` | ADMIN | Quản trị viên hệ thống |
| `owner` | `admin123` | OWNER | Chủ cửa hàng |
| `test` | `admin123` | EMPLOYEE | Nhân viên thử nghiệm |

### 📝 Hướng Dẫn Đăng Nhập

1. Truy cập: http://localhost:3000
2. Nhập tên đăng nhập: **admin**
3. Nhập mật khẩu: **admin123**
4. Nhấn "ĐĂNG NHẬP"

### ✨ Đã Sửa

- ✅ Cập nhật password hash đúng trong database (bizflow_auth_db)
- ✅ Kiểm tra API authentication service hoạt động (port 8086)
- ✅ Kiểm tra Gateway routing đúng (port 8000)
- ✅ Kiểm tra Nginx proxy đúng (port 3000)
- ✅ Test đăng nhập thành công với admin/admin123

### 🔍 Endpoint API

- **Frontend**: http://localhost:3000
- **Gateway**: http://localhost:8000
- **Auth Service**: http://localhost:8086/api/auth/login

### 🧪 Test API Trực Tiếp

```bash
# Test qua authentication service trực tiếp
curl -X POST http://localhost:8086/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Test qua gateway
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Test qua nginx (như frontend)
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### ⚠️ Lưu Ý Quan Trọng

1. Mật khẩu đã được cập nhật trong database
2. Password hash sử dụng BCrypt: `$2a$10$0etWynf6qtZjxYMTKREGmeZPT4nTUCkIMZEPTeKl6pmOMX18g.wFm`
3. Nếu vẫn gặp lỗi, hãy xóa cache trình duyệt (Ctrl + Shift + Delete)
4. Password này chỉ dùng cho môi trường development, KHÔNG dùng cho production

### 🔄 Nếu Cần Reset Password

```sql
-- Kết nối vào MySQL container
docker-compose exec mysql mysql -uroot -p123456

-- Chọn database
USE bizflow_auth_db;

-- Update password (password: admin123)
UPDATE users SET password='$2a$10$0etWynf6qtZjxYMTKREGmeZPT4nTUCkIMZEPTeKl6pmOMX18g.wFm' 
WHERE username='admin';
```

### 📌 Các Tài Khoản Khác Trong Hệ Thống

Các tài khoản này có thể có password khác, cần kiểm tra trong database:

- `vietphd` - EMPLOYEE
- `Tutl` - EMPLOYEE  
- `TanBinh` - OWNER

---
**Cập nhật lần cuối**: 29/01/2026
**Trạng thái**: ✅ Hoạt động bình thường
