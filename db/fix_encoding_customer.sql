-- Fix encoding cho Customer DB
SET NAMES utf8mb4;
SET CHARACTER_SET_CLIENT = utf8mb4;
SET CHARACTER_SET_CONNECTION = utf8mb4;
SET CHARACTER_SET_RESULTS = utf8mb4;
SET COLLATION_CONNECTION = utf8mb4_unicode_ci;

USE bizflow_customer_db;

-- Xóa và import lại với encoding đúng
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE point_history;
TRUNCATE TABLE customers;

-- Import customers với tiếng Việt
INSERT INTO `customers` (`id`, `address`, `email`, `name`, `phone`, `cccd`, `dob`, `monthly_points`, `tier`, `total_points`) VALUES
(1, 'Tân Bình', NULL, 'Phạm Việt', '0866066042', NULL, NULL, 44, 'DONG', 44),
(2, 'Tân Bình', NULL, 'Anh Thái', '0866066043', NULL, NULL, 7518, 'DONG', 7518),
(5, 'Chung cư', NULL, 'Anh Tứ', '0866066044', NULL, NULL, 870, 'DONG', 870),
(6, 'Tân Bình', NULL, 'Chị Vân', '0866066045', NULL, NULL, 243, 'DONG', 243),
(7, 'Test Address', NULL, 'Test Customer', '0962028826', NULL, NULL, 0, 'DONG', 0),
(8, 'Test Address', NULL, 'Test Customer 2', '0928519177', NULL, NULL, 10, 'DONG', 10),
(10, 'Test Address', NULL, 'Test UI Flow', '0996622189', NULL, NULL, 0, 'DONG', 0),
(11, 'Tân Chánh Hiệp', NULL, 'Anh Trung', '0354970825', NULL, NULL, 0, 'DONG', 0);

SET FOREIGN_KEY_CHECKS = 1;

SELECT '✓ Customer DB fixed with UTF8MB4 encoding' as message;
SELECT id, name, address, phone, total_points FROM customers;
