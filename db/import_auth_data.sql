-- Import trực tiếp dữ liệu vào các bảng (không tạo lại table)
-- Sử dụng REPLACE để tránh lỗi duplicate key

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';

USE bizflow_auth_db;

-- Insert branches trước (vì users có foreign key đến branches)
REPLACE INTO `branches` (`id`, `address`, `email`, `is_active`, `name`, `phone`, `owner_id`) VALUES
(1, 'TÂN CHÁNH HIỆP', 'gtvt@gmail.com', b'1', 'GTVT', '0981764731', NULL),
(2, '123 Street', 'sadanhthue01@gmail.com', b'1', 'Tân Bình', '0981764731', 2);

-- Insert users
REPLACE INTO `users` (`id`, `username`, `password`, `email`, `full_name`, `phone_number`, `role`, `enabled`, `branch_id`, `created_at`, `updated_at`, `note`) VALUES
(1, 'admin', '$2a$10$7gz3idM0iA0ikYyibDutqe31yrWDdVh2NIRa1gCj0QXVNw9723f0G', 'admin@bizflow.com', 'Administrator', NULL, 'ADMIN', 1, NULL, '2025-12-21 10:47:36', '2025-12-21 10:47:36', NULL),
(2, 'owner', '$2a$10$iDS5.CarVV4hxkD1P5oVYePzl/M8gs3jse7bGOAjhQBZ6iefSllWy', 'owner@bizflow.com', 'Store Owner', NULL, 'OWNER', 1, NULL, '2025-12-21 10:47:36', '2025-12-21 10:47:36', NULL),
(3, 'test', '$2a$10$iDS5.CarVV4hxkD1P5oVYePzl/M8gs3jse7bGOAjhQBZ6iefSllWy', 'test@bizflow.com', 'Test User', NULL, 'EMPLOYEE', 1, NULL, '2025-12-21 10:47:36', '2025-12-21 10:47:36', NULL),
(4, 'vietphd', '$2a$10$hTmAfVr7LjuSr5AxSKrpJeleoHtsiZn1RuVH9jub038t4C5SAIhiq', 'nhanvien1@gmail.com', 'Phạm Huy Đức Việt', '0902313141', 'EMPLOYEE', 1, NULL, '2025-12-24 16:44:38', '2026-01-27 08:36:31', NULL),
(7, 'Tutl', '$2a$10$0P6niSx/VIjhEfnjFVv.cOWuRpb.WTEhAvCEdTzUO9BFyuDVwp2je', 'Tutl@gmail.com', 'Trần Long Tú', '0866066043', 'EMPLOYEE', 1, NULL, '2026-01-03 21:57:14', '2026-01-27 08:36:31', NULL),
(8, 'TanBinh', '$2a$10$C2DybkhUAxwMFkLTSIVnteX834ZBW/Glnvg2OKPZxVzNZJvAYCIyW', 'tanbinh@gmail.com', 'Tấn Bình', '0866066042', 'OWNER', 1, 2, '2026-01-21 22:25:46', '2026-01-27 08:36:31', NULL);

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Auth DB imported successfully!' as message;
SELECT CONCAT('Users: ', COUNT(*)) as result FROM users;
SELECT CONCAT('Branches: ', COUNT(*)) as result FROM branches;
