-- =====================================================
-- ADMIN SERVICES INITIALIZATION
-- Tạo database riêng cho các Admin Service
-- =====================================================

-- =====================================================
-- 1. ADMIN USER SERVICE DATABASE
-- =====================================================
CREATE DATABASE IF NOT EXISTS bizflow_admin_user_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE bizflow_admin_user_db;

-- Admin Users Table
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'STAFF',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    branch_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role (role),
    INDEX idx_status (status),
    INDEX idx_branch_id (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample admin users
INSERT INTO admin_users (username, email, full_name, role, status, branch_id) VALUES
('admin', 'admin@bizflow.com', 'Quản trị viên chính', 'ADMIN', 'ACTIVE', 1),
('manager01', 'manager01@bizflow.com', 'Trần Văn A', 'MANAGER', 'ACTIVE', 1),
('manager02', 'manager02@bizflow.com', 'Nguyễn Thị B', 'MANAGER', 'ACTIVE', 2),
('staff01', 'staff01@bizflow.com', 'Lê Văn C', 'STAFF', 'ACTIVE', 1),
('staff02', 'staff02@bizflow.com', 'Phạm Thị D', 'STAFF', 'ACTIVE', 1),
('staff03', 'staff03@bizflow.com', 'Hoàng Văn E', 'STAFF', 'ACTIVE', 2),
('user01', 'user01@bizflow.com', 'Võ Thị F', 'USER', 'ACTIVE', 1),
('user02', 'user02@bizflow.com', 'Đặng Văn G', 'USER', 'INACTIVE', 2)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- =====================================================
-- 2. ADMIN PRODUCT SERVICE DATABASE
-- =====================================================
CREATE DATABASE IF NOT EXISTS bizflow_admin_product_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE bizflow_admin_product_db;

-- Admin Products Table
CREATE TABLE IF NOT EXISTS admin_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) UNIQUE NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    price DECIMAL(15, 2) NOT NULL,
    stock INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_price (price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample products
INSERT INTO admin_products (name, sku, category, description, price, stock, status) VALUES
('Laptop Dell XPS 13', 'LAPTOP-DELL-001', 'ELECTRONICS', 'Laptop cao cấp 13 inch', 25000000, 15, 'ACTIVE'),
('MacBook Pro 14', 'LAPTOP-MAC-001', 'ELECTRONICS', 'Laptop MacBook Pro 14 inch M1', 35000000, 8, 'ACTIVE'),
('iPhone 15 Pro', 'PHONE-APPLE-001', 'ELECTRONICS', 'Điện thoại iPhone 15 Pro Max', 30000000, 20, 'ACTIVE'),
('Samsung Galaxy S24', 'PHONE-SAMSUNG-001', 'ELECTRONICS', 'Điện thoại Samsung Galaxy S24', 22000000, 25, 'ACTIVE'),
('Áo Polo nam', 'SHIRT-POLO-001', 'CLOTHING', 'Áo Polo nam cotton 100%', 350000, 100, 'ACTIVE'),
('Quần Jeans nam', 'PANTS-JEANS-001', 'CLOTHING', 'Quần Jeans nam Levi''s', 450000, 75, 'ACTIVE'),
('Giày Nike Air', 'SHOES-NIKE-001', 'CLOTHING', 'Giày Nike Air Force 1', 1200000, 50, 'ACTIVE'),
('Bánh mì thường', 'FOOD-BREAD-001', 'FOOD', 'Bánh mì Pháp truyền thống', 25000, 200, 'ACTIVE'),
('Cà phê hạt', 'FOOD-COFFEE-001', 'FOOD', 'Cà phê hạt nguyên chất 100%', 120000, 150, 'ACTIVE'),
('Nước trái cây', 'FOOD-JUICE-001', 'FOOD', 'Nước cam tươi', 35000, 300, 'ACTIVE'),
('Kem dưỡng da', 'BEAUTY-CREAM-001', 'BEAUTY', 'Kem dưỡng da chống lão hóa', 450000, 60, 'ACTIVE'),
('Son môi Lipstick', 'BEAUTY-LIPSTICK-001', 'BEAUTY', 'Son môi màu đỏ sang chảnh', 150000, 80, 'ACTIVE'),
('Bóng đá size 5', 'SPORTS-BALL-001', 'SPORTS', 'Bóng đá chính hãng', 350000, 40, 'ACTIVE'),
('Vợt cầu lông', 'SPORTS-RACKET-001', 'SPORTS', 'Vợt cầu lông carbon', 750000, 30, 'ACTIVE'),
('Sách Lập trình Python', 'BOOK-PYTHON-001', 'BOOKS', 'Sách học lập trình Python', 120000, 100, 'ACTIVE')
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- =====================================================
-- 3. ADMIN ORDER SERVICE DATABASE
-- =====================================================
CREATE DATABASE IF NOT EXISTS bizflow_admin_order_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE bizflow_admin_order_db;

-- Admin Orders Table
CREATE TABLE IF NOT EXISTS admin_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(100),
    customer_phone VARCHAR(20),
    total_amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    shipping_address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_customer_email (customer_email),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample orders
INSERT INTO admin_orders (order_number, customer_name, customer_email, customer_phone, total_amount, status, payment_method, shipping_address, created_at) VALUES
('ORD-2024-0001', 'Trần Văn Anh', 'anh@example.com', '0912345678', 25000000, 'DELIVERED', 'CREDIT_CARD', '123 Đường Lê Lợi, TP.HCM', DATE_SUB(NOW(), INTERVAL 30 DAY)),
('ORD-2024-0002', 'Nguyễn Thị Bình', 'binh@example.com', '0987654321', 1200000, 'DELIVERED', 'BANK_TRANSFER', '456 Đường Nguyễn Huệ, TP.HCM', DATE_SUB(NOW(), INTERVAL 25 DAY)),
('ORD-2024-0003', 'Hoàng Văn Cường', 'cuong@example.com', '0909090909', 2850000, 'SHIPPED', 'COD', '789 Đường Hùng Vương, Hà Nội', DATE_SUB(NOW(), INTERVAL 15 DAY)),
('ORD-2024-0004', 'Phạm Thị Dũng', 'dung@example.com', '0933333333', 450000, 'PROCESSING', 'CREDIT_CARD', '321 Đường Lý Thánh Tông, TP.HCM', DATE_SUB(NOW(), INTERVAL 5 DAY)),
('ORD-2024-0005', 'Lê Văn Em', 'em@example.com', '0944444444', 5250000, 'PROCESSING', 'BANK_TRANSFER', '654 Đường Trần Hưng Đạo, Hà Nội', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('ORD-2024-0006', 'Võ Thị Hương', 'huong@example.com', '0955555555', 3600000, 'PENDING', 'COD', '987 Đường Ông Ích Khiêm, TP.HCM', NOW()),
('ORD-2024-0007', 'Đặng Văn Kiên', 'kien@example.com', '0966666666', 1575000, 'PENDING', 'CREDIT_CARD', '147 Đường Hoàng Văn Thụ, Hà Nội', NOW()),
('ORD-2024-0008', 'Tạ Thị Loan', 'loan@example.com', '0977777777', 750000, 'CANCELLED', 'BANK_TRANSFER', '258 Đường Trần Phú, Đà Nẵng', DATE_SUB(NOW(), INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- =====================================================
-- 4. ADMIN REPORT SERVICE DATABASE
-- =====================================================
CREATE DATABASE IF NOT EXISTS bizflow_admin_report_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE bizflow_admin_report_db;

-- Admin Reports Table
CREATE TABLE IF NOT EXISTS admin_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    record_count INT DEFAULT 0,
    status VARCHAR(30) DEFAULT 'COMPLETED',
    generated_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample reports
INSERT INTO admin_reports (title, description, type, record_count, status, generated_by, created_at) VALUES
('Báo cáo doanh thu tháng 1/2024', 'Tổng hợp doanh thu các chi nhánh tháng 1', 'SALES', 8, 'COMPLETED', 'admin', DATE_SUB(NOW(), INTERVAL 30 DAY)),
('Báo cáo hàng tồn tháng 1/2024', 'Thống kê hàng tồn kho tháng 1', 'INVENTORY', 15, 'COMPLETED', 'manager01', DATE_SUB(NOW(), INTERVAL 28 DAY)),
('Báo cáo khách hàng mới tháng 1', 'Phân tích khách hàng mới tháng 1/2024', 'CUSTOMER', 156, 'COMPLETED', 'admin', DATE_SUB(NOW(), INTERVAL 25 DAY)),
('Báo cáo khuyến mãi tháng 1/2024', 'Hiệu quả các chiến dịch khuyến mãi', 'PROMOTION', 5, 'COMPLETED', 'manager02', DATE_SUB(NOW(), INTERVAL 20 DAY)),
('Báo cáo tổng hợp tháng 1/2024', 'Báo cáo tổng hợp toàn hệ thống', 'SUMMARY', 1, 'COMPLETED', 'admin', DATE_SUB(NOW(), INTERVAL 15 DAY)),
('Báo cáo phân tích doanh số tuần 1/2', 'Phân tích chi tiết doanh số từng ngày', 'ANALYSIS', 7, 'COMPLETED', 'manager01', DATE_SUB(NOW(), INTERVAL 10 DAY)),
('Báo cáo doanh thu tuần 2/2', 'Tổng hợp doanh thu tuần 2 tháng 2', 'SALES', 8, 'COMPLETED', 'admin', DATE_SUB(NOW(), INTERVAL 5 DAY)),
('Báo cáo hàng tồn hiện tại', 'Thống kê tình trạng hàng tồn thời điểm hiện tại', 'INVENTORY', 15, 'COMPLETED', 'staff01', NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- =====================================================
-- SUMMARY
-- =====================================================
SELECT 'Admin Services Initialization Completed!' AS status;
SELECT COUNT(*) AS admin_users FROM bizflow_admin_user_db.admin_users;
SELECT COUNT(*) AS admin_products FROM bizflow_admin_product_db.admin_products;
SELECT COUNT(*) AS admin_orders FROM bizflow_admin_order_db.admin_orders;
SELECT COUNT(*) AS admin_reports FROM bizflow_admin_report_db.admin_reports;
