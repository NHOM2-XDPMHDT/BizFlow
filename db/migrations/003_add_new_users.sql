-- Migration: Add new users
-- Date: 2026-01-03
-- Author: Phạm Huy Đức Việt

-- Thêm user mới
INSERT INTO users (username, password, full_name, email, role, enabled) 
VALUES 
  ('newuser', '$2a$10$...', 'New User Name', 'new@example.com', 'EMPLOYEE', true)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- Thêm sản phẩm mới
INSERT INTO products (sku, product_name, price, status)
VALUES
  ('SKU001', 'Sản phẩm mới', 100000, 'active')
ON DUPLICATE KEY UPDATE price = VALUES(price);
