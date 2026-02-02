-- =====================================================
-- MIGRATION: Cập nhật table SHELVES cho cơ chế kệ hàng
-- =====================================================
-- Mục đích: Loại bỏ warehouses, thêm cơ chế kệ hàng
-- OWNER đưa hàng từ inventory_stock lên shelves
-- Nhân viên chỉ bán từ shelves
-- =====================================================

USE bizflow_inventory_db;

-- Xóa table shelves cũ nếu tồn tại
DROP TABLE IF EXISTS `shelves`;

-- Tạo table shelves mới
CREATE TABLE `shelves` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT 'ID sản phẩm trên kệ',
  `quantity` int NOT NULL DEFAULT 0 COMMENT 'Số lượng sản phẩm trên kệ',
  `created_at` datetime DEFAULT NULL COMMENT 'Thời gian tạo',
  `updated_at` datetime DEFAULT NULL COMMENT 'Thời gian cập nhật',
  `updated_by` bigint DEFAULT NULL COMMENT 'ID người cập nhật',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shelf_product` (`product_id`),
  KEY `idx_shelf_quantity` (`quantity`),
  KEY `idx_shelf_updated` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng quản lý kệ hàng - chỉ bán từ kệ';

-- Thêm comment cho bảng
ALTER TABLE `shelves` COMMENT = 'Kệ hàng - OWNER đưa từ kho lên, nhân viên bán từ kệ';

-- Tự động xóa khi quantity = 0 (sẽ xử lý trong application logic)

SELECT 'Migration completed: shelves table recreated for new shelf management system' AS Status;
