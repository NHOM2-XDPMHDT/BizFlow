-- MySQL dump 10.13  Distrib 8.0.44, for Linux (x86_64)
--
-- Host: localhost    Database: bizflow_db
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `branches`
--

DROP TABLE IF EXISTS `branches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `branches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `owner_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_hw68nd07qk3jrjfg70qxq9vb7` (`name`),
  KEY `FK8lecw87wgj5h4k0x8klg4bnep` (`owner_id`),
  CONSTRAINT `FK8lecw87wgj5h4k0x8klg4bnep` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branches`
--

LOCK TABLES `branches` WRITE;
/*!40000 ALTER TABLE `branches` DISABLE KEYS */;
/*!40000 ALTER TABLE `branches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `category_id` int NOT NULL AUTO_INCREMENT,
  `category_name` varchar(255) NOT NULL,
  `parent_id` int DEFAULT NULL,
  `status` enum('active','inactive') DEFAULT 'active',
  PRIMARY KEY (`category_id`),
  KEY `fk_category_parent` (`parent_id`),
  CONSTRAINT `fk_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`category_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,'Nước Giải Khát',NULL,'active'),(2,'Đồ Ăn Vặt',NULL,'active'),(3,'Hóa Mỹ Phẩm',NULL,'active'),(4,'Gia Vị & Nước Chấm',NULL,'active'),(5,'Sản Phẩm Chăm Sóc Nhà Cửa',NULL,'active'),(6,'Bánh Kẹo',NULL,'active'),(7,'Bia & Rượu',NULL,'active'),(8,'Mì, Phở, Cháo Gói',NULL,'active'),(9,'Đồ Hộp & Thực Phẩm Đóng Hộp',NULL,'active'),(10,'Thuốc Lá & Diêm',NULL,'active');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customers`
--

DROP TABLE IF EXISTS `customers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customers`
--

LOCK TABLES `customers` WRITE;
/*!40000 ALTER TABLE `customers` DISABLE KEYS */;
/*!40000 ALTER TABLE `customers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_transactions`
--

DROP TABLE IF EXISTS `inventory_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_transactions` (
  `transaction_id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` int NOT NULL,
  `warehouse_id` bigint DEFAULT NULL,
  `shelf_id` bigint DEFAULT NULL,
  `transaction_type` enum('IN','OUT','SALE','RETURN','ADJUST') NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(12,2) DEFAULT NULL,
  `reference_type` varchar(50) DEFAULT NULL,
  `reference_id` bigint DEFAULT NULL,
  `note` text,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`transaction_id`),
  KEY `fk_it_product` (`product_id`),
  KEY `fk_it_warehouse` (`warehouse_id`),
  KEY `fk_it_shelf` (`shelf_id`),
  KEY `fk_it_created_by` (`created_by`),
  CONSTRAINT `fk_it_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_it_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_it_shelf` FOREIGN KEY (`shelf_id`) REFERENCES `shelves` (`shelf_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_it_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses` (`warehouse_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_transactions`
--

LOCK TABLES `inventory_transactions` WRITE;
/*!40000 ALTER TABLE `inventory_transactions` DISABLE KEYS */;
/*!40000 ALTER TABLE `inventory_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `price` decimal(15,2) NOT NULL,
  `quantity` int NOT NULL,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
  CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `invoice_number` varchar(30) DEFAULT NULL,
  `status` varchar(30) DEFAULT NULL,
  `is_return` bit(1) DEFAULT NULL,
  `order_type` varchar(20) DEFAULT NULL,
  `parent_order_id` bigint DEFAULT NULL,
  `return_reason` varchar(255) DEFAULT NULL,
  `return_note` varchar(255) DEFAULT NULL,
  `refund_method` varchar(50) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  `total_amount` decimal(15,2) NOT NULL,
  `customer_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_orders_parent` (`parent_order_id`),
  KEY `FKpxtb8awmi0dk6smoh2vp1litg` (`customer_id`),
  KEY `FK32ql8ubntj5uh44ph9659tiih` (`user_id`),
  CONSTRAINT `FK_orders_parent` FOREIGN KEY (`parent_order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FK32ql8ubntj5uh44ph9659tiih` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKpxtb8awmi0dk6smoh2vp1litg` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `amount` decimal(15,2) NOT NULL,
  `method` varchar(50) NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `order_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK81gagumt0r8y3rmudcgpbk42l` (`order_id`),
  CONSTRAINT `FK81gagumt0r8y3rmudcgpbk42l` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `product_id` int NOT NULL AUTO_INCREMENT,
  `product_name` varchar(255) NOT NULL,
  `sku` varchar(100) NOT NULL,
  `barcode` varchar(100) DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `unit` varchar(50) NOT NULL,
  `price` decimal(15,2) NOT NULL,
  `cost_price` decimal(12,2) DEFAULT NULL,
  `status` enum('active','inactive') DEFAULT 'active',
  `description` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `code` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `sku` (`sku`),
  KEY `fk_product_category` (`category_id`),
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=151 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,'Coca-Cola lon 330ml','CC330','8934567000010',1,'lon',10000.00,7500.00,'active','Nước ngọt có ga','2025-12-29 17:04:24',NULL,NULL,'',''),(2,'Trà Xanh Không Độ chai 500ml','TXKD500','8934567000027',1,'chai',12000.00,9000.00,'active','Trà giải khát không đường','2025-12-29 17:04:24',NULL,NULL,'',''),(3,'Nước suối Aquafina 500ml','AQF500','8934567000034',1,'chai',5000.00,3500.00,'active','Nước tinh khiết','2025-12-29 17:04:24',NULL,NULL,'',''),(4,'Bia Sài Gòn Lager 330ml','SGL330','8934567000041',1,'lon',15000.00,11000.00,'active','Bia Lager (có thể dùng chung ID 7 nếu không muốn tách)','2025-12-29 17:04:24',NULL,NULL,'',''),(5,'Pepsi lon 330ml','PS330','8934567000058',1,'lon',10000.00,7500.00,'active','Nước ngọt có ga vị chanh','2025-12-29 17:04:24',NULL,NULL,'',''),(6,'Snack Oishi vị Phô Mai 35g','OIS-PM35','8934567000065',2,'gói',8000.00,5500.00,'active','Bánh snack khoai tây','2025-12-29 17:04:24',NULL,NULL,'',''),(7,'Khô Gà Lá Chanh 100g','KG-LC100','8934567000072',2,'túi',35000.00,25000.00,'active','Thực phẩm ăn liền','2025-12-29 17:04:24',NULL,NULL,'',''),(8,'Hạt Hướng Dương Vị Muối 250g','HD-MS250','8934567000089',2,'gói',20000.00,14000.00,'active','Hạt rang ăn liền','2025-12-29 17:04:24',NULL,NULL,'',''),(9,'Bánh Quy Cosy dừa 160g','CQ-DY160','8934567000096',2,'gói',18000.00,12500.00,'active','Bánh quy ngọt','2025-12-29 17:04:24',NULL,NULL,'',''),(10,'Kẹo Alpenliebe vị caramen','ALP-CR','8934567000102',2,'gói',15000.00,10000.00,'active','Kẹo cứng (có thể dùng chung ID 6)','2025-12-29 17:04:24',NULL,NULL,'',''),(11,'Dầu Gội Sunsilk Mềm Mượt 320g','DG-SM320','8934567000119',3,'chai',65000.00,45000.00,'active','Dầu gội đầu','2025-12-29 17:04:24',NULL,NULL,'',''),(12,'Kem Đánh Răng P/S Bảo Vệ 120g','KDR-PSBV','8934567000126',3,'tuýp',30000.00,20000.00,'active','Kem đánh răng cơ bản','2025-12-29 17:04:24',NULL,NULL,'',''),(13,'Xà Bông Lifebuoy diệt khuẩn 90g','XB-LB90','8934567000133',3,'bánh',15000.00,10000.00,'active','Xà bông tắm/rửa tay','2025-12-29 17:04:24',NULL,NULL,'',''),(14,'Dao cạo râu Gillette 1 lưỡi','DCR-GL1','8934567000140',3,'cái',5000.00,3000.00,'active','Dụng cụ cá nhân','2025-12-29 17:04:24',NULL,NULL,'',''),(15,'Khăn Giấy Gấu Trúc 180 tờ','KG-GT180','8934567000157',3,'gói',12000.00,8000.00,'active','Khăn giấy khô','2025-12-29 17:04:24',NULL,NULL,'',''),(16,'Nước Mắm Chin-su 500ml','NM-CS500','8934567000164',4,'chai',45000.00,30000.00,'active','Nước mắm cá cơm','2025-12-29 17:04:24',NULL,NULL,'',''),(17,'Dầu Ăn Tường An 1 Lít','DA-TA1L','8934567000171',4,'chai',60000.00,40000.00,'active','Dầu thực vật','2025-12-29 17:04:24',NULL,NULL,'',''),(18,'Muối I-ốt Bạc Liêu 500g','M-BL500','8934567000188',4,'gói',5000.00,3000.00,'active','Muối ăn thông thường','2025-12-29 17:04:24',NULL,NULL,'',''),(19,'Đường Trắng Biên Hòa 1kg','DT-BH1KG','8934567000195',4,'gói',25000.00,18000.00,'active','Đường tinh luyện','2025-12-29 17:04:24',NULL,NULL,'',''),(20,'Bột Ngọt Ajinomoto 450g','BN-AJI450','8934567000201',4,'gói',35000.00,25000.00,'active','Chất điều vị','2025-12-29 17:04:24',NULL,NULL,'',''),(21,'Nước Rửa Chén Mỹ Hảo 800g','NRC-MH800','8934567000218',5,'chai',25000.00,15000.00,'active','Nước rửa chén bát','2025-12-29 17:04:24',NULL,NULL,'',''),(22,'Bột Giặt OMO Matic 800g','BG-OMO800','8934567000225',5,'túi',45000.00,30000.00,'active','Bột giặt máy giặt','2025-12-29 17:04:24',NULL,NULL,'',''),(23,'Nước Lau Sàn Gift 1 Lít','NLS-GF1L','8934567000232',5,'chai',35000.00,22000.00,'active','Chất tẩy rửa sàn nhà','2025-12-29 17:04:24',NULL,NULL,'',''),(24,'Thuốc diệt muỗi Mosfly chai','TDM-MOS','8934567000249',5,'chai',70000.00,50000.00,'active','Chất diệt côn trùng','2025-12-29 17:04:24',NULL,NULL,'',''),(25,'Túi đựng rác tự phân hủy (3 cuộn)','TDR-3C','8934567000256',5,'gói',15000.00,10000.00,'active','Dụng cụ vệ sinh','2025-12-29 17:04:24',NULL,NULL,'',''),(26,'Bánh Chocopie Hộp 12 cái','CP-12C','8934567000263',6,'hộp',40000.00,28000.00,'active','Bánh xốp phủ sô cô la','2025-12-29 17:04:24',NULL,NULL,'',''),(27,'Kẹo Sugus Trái Cây 150g','K-SUG150','8934567000270',6,'túi',20000.00,13000.00,'active','Kẹo dẻo mềm','2025-12-29 17:04:24',NULL,NULL,'',''),(28,'Bánh Mì Sữa Tươi Kinh Đô','BM-KDS','8934567000287',6,'gói',12000.00,8000.00,'active','Bánh mì ngọt ăn sáng','2025-12-29 17:04:24',NULL,NULL,'',''),(29,'Thạch Rau Câu Long Hải 400g','TRC-LH400','8934567000294',6,'túi',15000.00,10000.00,'active','Tráng miệng lạnh','2025-12-29 17:04:24',NULL,NULL,'',''),(30,'Kẹo Socola Kitkat thanh','SC-KKTH','8934567000300',6,'thanh',8000.00,5000.00,'active','Bánh xốp phủ socola','2025-12-29 17:04:24',NULL,NULL,'',''),(31,'Bia Tiger lon 330ml','BT-330','8934567000317',7,'lon',17000.00,12500.00,'active','Bia Lager cao cấp','2025-12-29 17:04:24',NULL,NULL,'',''),(32,'Bia Heineken lon 330ml','BH-330','8934567000324',7,'lon',20000.00,15000.00,'active','Bia nhập khẩu','2025-12-29 17:04:24',NULL,NULL,'',''),(33,'Rượu Vodka Hà Nội 700ml','R-VDHN','8934567000331',7,'chai',150000.00,100000.00,'active','Rượu mạnh','2025-12-29 17:04:24',NULL,NULL,'',''),(34,'Bia 333 lon 330ml','B3-330','8934567000348',7,'lon',14000.00,10000.00,'active','Bia truyền thống','2025-12-29 17:04:24',NULL,NULL,'',''),(35,'Bia Larue lon 330ml','BL-330','8934567000355',7,'lon',13000.00,9000.00,'active','Bia phổ thông','2025-12-29 17:04:24',NULL,NULL,'',''),(36,'Mì Hảo Hảo Tôm Chua Cay','MHHTC','8934567000362',8,'gói',6000.00,4000.00,'active','Mì ăn liền phổ thông','2025-12-29 17:04:24',NULL,NULL,'',''),(37,'Phở Bò Gói Vifon','PB-VF','8934567000379',8,'gói',12000.00,8000.00,'active','Phở ăn liền','2025-12-29 17:04:24',NULL,NULL,'',''),(38,'Mì Omachi Xốt Thịt Heo','M-OMXTH','8934567000386',8,'gói',10000.00,6500.00,'active','Mì không chiên','2025-12-29 17:04:24',NULL,NULL,'',''),(39,'Cháo Thịt Bằm gói 50g','CB-50G','8934567000393',8,'gói',8000.00,5000.00,'active','Cháo ăn liền dinh dưỡng','2025-12-29 17:04:24',NULL,NULL,'',''),(40,'Mì Kokomi Đại gói','MKD-GOI','8934567000409',8,'gói',5500.00,3500.00,'active','Mì gói lớn','2025-12-29 17:04:24',NULL,NULL,'',''),(41,'Pate Cột Đèn Hải Phòng 100g','PCĐ-100','8934567000416',9,'hộp',25000.00,18000.00,'active','Thịt xay đóng hộp','2025-12-29 17:04:24',NULL,NULL,'',''),(42,'Cá Hộp Sốt Cà Chua 3 Cô Gái','CH-3CG','8934567000423',9,'hộp',20000.00,14000.00,'active','Cá hộp ăn liền','2025-12-29 17:04:24',NULL,NULL,'',''),(43,'Thịt Heo 2 Lát Đóng Hộp','TH2L-DH','8934567000430',9,'hộp',40000.00,28000.00,'active','Thịt heo chế biến sẵn','2025-12-29 17:04:24',NULL,NULL,'',''),(44,'Sữa Đặc Ông Thọ Vàng 380g','SD-OTV','8934567000447',9,'lon',28000.00,20000.00,'active','Sữa đặc có đường','2025-12-29 17:04:24',NULL,NULL,'',''),(45,'Dưa Chuột Muối chua 500g','DCM-500','8934567000454',9,'hũ',30000.00,21000.00,'active','Rau củ muối chua','2025-12-29 17:04:24',NULL,NULL,'',''),(46,'Thuốc Lá Vinataba Gói','TL-VNTB','8934567000461',10,'gói',30000.00,22000.00,'active','Thuốc lá thông dụng','2025-12-29 17:04:24',NULL,NULL,'',''),(47,'Thuốc Lá 555 Gói','TL-555','8934567000478',10,'gói',35000.00,26000.00,'active','Thuốc lá cao cấp hơn','2025-12-29 17:04:24',NULL,NULL,'',''),(48,'Bật Lửa Gas BIC (màu xanh)','BL-BICX','8934567000485',10,'cái',8000.00,5000.00,'active','Bật lửa dùng 1 lần','2025-12-29 17:04:24',NULL,NULL,'',''),(49,'Diêm Thống Nhất Hộp Nhỏ','D-TN','8934567000492',10,'hộp',2000.00,1000.00,'active','Diêm quẹt','2025-12-29 17:04:24',NULL,NULL,'',''),(50,'Thuốc Lá Kent Gói','TL-KNT','8934567000508',10,'gói',40000.00,30000.00,'active','Thuốc lá nhập khẩu','2025-12-29 17:04:24',NULL,NULL,'',''),(51,'Nước tăng lực Red Bull 250ml','NTL-RB','8934567000515',1,'lon',18000.00,13000.00,'active','Nước uống tăng lực Thái Lan','2025-12-29 17:08:00',NULL,NULL,'',''),(52,'Sữa tươi Vinamilk không đường 1L','ST-VNM-KS','8934567000522',1,'hộp',35000.00,25000.00,'active','Sữa tươi tiệt trùng 100%','2025-12-29 17:08:00',NULL,NULL,'',''),(53,'Nước ép cam Vfresh 1L','NE-VFC','8934567000539',1,'hộp',40000.00,28000.00,'active','Nước ép trái cây nguyên chất','2025-12-29 17:08:00',NULL,NULL,'',''),(54,'Nước suối Lavie 1.5L','NUC-LV15','8934567000546',1,'chai',8000.00,5000.00,'active','Nước khoáng thiên nhiên','2025-12-29 17:08:00',NULL,NULL,'',''),(55,'Nước yến ngân nhĩ 240ml','NY-NN240','8934567000553',1,'lon',25000.00,18000.00,'active','Nước giải khát bổ dưỡng','2025-12-29 17:08:00',NULL,NULL,'',''),(56,'Bia 333 thùng 24 lon','B333-T24','8934567000560',1,'thùng',320000.00,250000.00,'active','Bia thùng 24 lon','2025-12-29 17:08:00',NULL,NULL,'',''),(57,'Trà đào Cozy hộp 25 gói','TD-CZY25','8934567000577',1,'hộp',45000.00,32000.00,'active','Trà hòa tan vị đào','2025-12-29 17:08:00',NULL,NULL,'',''),(58,'Sting dâu lon 330ml','NTL-STING','8934567000584',1,'lon',12000.00,8500.00,'active','Nước tăng lực vị dâu','2025-12-29 17:08:00',NULL,NULL,'',''),(59,'Sữa chua uống Probi 65ml (lốc 5)','SCU-PB','8934567000591',1,'lốc',22000.00,16000.00,'active','Sữa chua uống lợi khuẩn','2025-12-29 17:08:00',NULL,NULL,'',''),(60,'Nước ép dứa Dole 330ml','NED-DL330','8934567000607',1,'lon',15000.00,10000.00,'active','Nước ép dứa đóng lon','2025-12-29 17:08:00',NULL,NULL,'',''),(61,'Bánh gạo One.One vị bò 100g','BG-OOVB','8934567000614',2,'gói',15000.00,10000.00,'active','Bánh gạo giòn tan','2025-12-29 17:08:00',NULL,NULL,'',''),(62,'Snack Poca vị muối ớt 70g','SK-POCA','8934567000621',2,'gói',18000.00,12000.00,'active','Snack khoai tây lát muối ớt','2025-12-29 17:08:00',NULL,NULL,'',''),(63,'Rong Biển Ăn Liền Taekyung 5g','RB-TK','8934567000638',2,'gói',7000.00,4500.00,'active','Rong biển sấy khô Hàn Quốc','2025-12-29 17:08:00',NULL,NULL,'',''),(64,'Hạt điều rang muối 500g','HD-RM500','8934567000645',2,'hộp',150000.00,110000.00,'active','Hạt điều Bình Phước','2025-12-29 17:08:00',NULL,NULL,'',''),(65,'Thịt bò khô miếng 50g','TBK-M50','8934567000652',2,'gói',45000.00,32000.00,'active','Thịt bò sấy khô xé sợi','2025-12-29 17:08:00',NULL,NULL,'',''),(66,'Bánh phồng tôm Sa Giang 200g','BPT-SG200','8934567000669',2,'gói',30000.00,20000.00,'active','Bánh phồng tôm chưa chiên','2025-12-29 17:08:00',NULL,NULL,'',''),(67,'Socola thanh Mars 50g','SC-MAR50','8934567000676',2,'thanh',18000.00,12000.00,'active','Socola nhân caramen','2025-12-29 17:08:00',NULL,NULL,'',''),(68,'Bim Bim Lay\'s vị tự nhiên 50g','BB-LAYS','8934567000683',2,'gói',10000.00,6500.00,'active','Snack khoai tây lát mỏng','2025-12-29 17:08:00',NULL,NULL,'',''),(69,'Kẹo dẻo Chuppa Chups 75g','KD-CC75','8934567000690',2,'gói',15000.00,10000.00,'active','Kẹo dẻo hình thù','2025-12-29 17:08:00',NULL,NULL,'',''),(70,'Mứt gừng sấy dẻo 200g','MG-SD200','8934567000706',2,'hộp',50000.00,35000.00,'active','Mứt gừng đặc sản','2025-12-29 17:08:00',NULL,NULL,'',''),(71,'Sữa tắm Enchanteur 450ml','ST-ECH','8934567000713',3,'chai',85000.00,60000.00,'active','Sữa tắm hương nước hoa','2025-12-29 17:08:00',NULL,NULL,'',''),(72,'Dầu xả TRESemmé 340g','DX-TRE','8934567000720',3,'chai',70000.00,50000.00,'active','Dầu xả dưỡng tóc óng mượt','2025-12-29 17:08:00',NULL,NULL,'',''),(73,'Kem chống nắng Bioré 50ml','KCN-BIO','8934567000737',3,'tuýp',90000.00,65000.00,'active','Bảo vệ da SPF50+','2025-12-29 17:08:00',NULL,NULL,'',''),(74,'Son dưỡng môi Vaseline 10g','SDM-VAS','8934567000744',3,'hộp',35000.00,25000.00,'active','Dưỡng ẩm môi khô nẻ','2025-12-29 17:08:00',NULL,NULL,'',''),(75,'Băng vệ sinh Diana Sensi 8 miếng','BVS-DS8','8934567000751',3,'gói',25000.00,18000.00,'active','Băng vệ sinh hàng ngày','2025-12-29 17:08:00',NULL,NULL,'',''),(76,'Nước tẩy trang L’Oréal 400ml','NTT-LO400','8934567000768',3,'chai',130000.00,90000.00,'active','Làm sạch sâu da mặt','2025-12-29 17:08:00',NULL,NULL,'',''),(77,'Mặt nạ giấy dưỡng ẩm (gói)','MN-GA','8934567000775',3,'gói',15000.00,10000.00,'active','Mặt nạ cấp ẩm','2025-12-29 17:08:00',NULL,NULL,'',''),(78,'Sữa rửa mặt Cetaphil 125ml','SRM-CET','8934567000782',3,'chai',80000.00,55000.00,'active','Sữa rửa mặt dịu nhẹ','2025-12-29 17:08:00',NULL,NULL,'',''),(79,'Khăn ướt Bobby không mùi (100 tờ)','KU-BBY','8934567000799',3,'gói',30000.00,20000.00,'active','Khăn ướt vệ sinh cá nhân','2025-12-29 17:08:00',NULL,NULL,'',''),(80,'Kem dưỡng ẩm Nivea Soft 50ml','KDA-NS50','8934567000805',3,'hộp',45000.00,30000.00,'active','Kem dưỡng ẩm toàn thân','2025-12-29 17:08:00',NULL,NULL,'',''),(81,'Tương ớt Chin-su chai 250g','TO-CS250','8934567000812',4,'chai',18000.00,12000.00,'active','Tương ớt cay nồng','2025-12-29 17:08:00',NULL,NULL,'',''),(82,'Xì Dầu Maggi chai 200ml','XD-MAG200','8934567000829',4,'chai',22000.00,15000.00,'active','Nước tương đậu nành','2025-12-29 17:08:00',NULL,NULL,'',''),(83,'Giấm gạo Ajinomoto 400ml','G-AJI400','8934567000836',4,'chai',15000.00,10000.00,'active','Giấm dùng nấu ăn','2025-12-29 17:08:00',NULL,NULL,'',''),(84,'Hạt nêm Knorr 400g','HN-KNR400','8934567000843',4,'gói',30000.00,21000.00,'active','Gia vị nêm nếm thịt thăn','2025-12-29 17:08:00',NULL,NULL,'',''),(85,'Tiêu xay Vifon 50g','TX-VF50','8934567000850',4,'hộp',25000.00,17000.00,'active','Bột tiêu xay nguyên chất','2025-12-29 17:08:00',NULL,NULL,'',''),(86,'Dầu hào Maggi 350g','DH-MAG350','8934567000867',4,'chai',35000.00,24000.00,'active','Dầu hào nấm hương','2025-12-29 17:08:00',NULL,NULL,'',''),(87,'Bột canh I-ốt 190g','BC-I-190','8934567000874',4,'gói',8000.00,5000.00,'active','Bột canh nêm nếm','2025-12-29 17:08:00',NULL,NULL,'',''),(88,'Nước tương Phú Sĩ 500ml','NT-PS500','8934567000881',4,'chai',18000.00,12000.00,'active','Nước tương phổ thông','2025-12-29 17:08:00',NULL,NULL,'',''),(89,'Dầu mè đen 200ml','DM-200','8934567000898',4,'chai',30000.00,21000.00,'active','Dầu mè thơm','2025-12-29 17:08:00',NULL,NULL,'',''),(90,'Bột mì đa dụng 500g','BM-500','8934567000904',4,'gói',15000.00,10000.00,'active','Bột dùng làm bánh','2025-12-29 17:08:00',NULL,NULL,'',''),(91,'Nước rửa tay Lifebuoy 450g','NRT-LB450','8934567000911',5,'chai',55000.00,38000.00,'active','Nước rửa tay diệt khuẩn','2025-12-29 17:08:00',NULL,NULL,'',''),(92,'Nước xả vải Comfort 800ml','NXV-CF800','8934567000928',5,'túi',40000.00,28000.00,'active','Làm mềm và thơm quần áo','2025-12-29 17:08:00',NULL,NULL,'',''),(93,'Giấy vệ sinh Sài Gòn 10 cuộn','GVS-SG10','8934567000935',5,'gói',50000.00,35000.00,'active','Giấy vệ sinh 3 lớp','2025-12-29 17:08:00',NULL,NULL,'',''),(94,'Dầu hỏa thắp sáng 500ml','DH-500','8934567000942',5,'chai',15000.00,10000.00,'active','Chất đốt thắp sáng','2025-12-29 17:08:00',NULL,NULL,'',''),(95,'Nến thơm Lavender','NT-LV','8934567000959',5,'cái',35000.00,24000.00,'active','Khử mùi, tạo hương thơm','2025-12-29 17:08:00',NULL,NULL,'',''),(96,'Nước tẩy bồn cầu Vim 450ml','NTBC-VM','8934567000966',5,'chai',30000.00,20000.00,'active','Chất tẩy rửa nhà vệ sinh','2025-12-29 17:08:00',NULL,NULL,'',''),(97,'Túi đựng rác 5kg (3 cuộn)','TDR-5KG','8934567000973',5,'gói',25000.00,17000.00,'active','Túi rác tự phân hủy','2025-12-29 17:08:00',NULL,NULL,'',''),(98,'Bột thông cống 100g','BTC-100','8934567000980',5,'gói',15000.00,10000.00,'active','Hóa chất thông cống','2025-12-29 17:08:00',NULL,NULL,'',''),(99,'Khăn lau đa năng 3M Scotch-Brite','KL-3MSB','8934567000997',5,'cái',40000.00,28000.00,'active','Khăn lau thấm nước','2025-12-29 17:08:00',NULL,NULL,'',''),(100,'Nước lau kính Cif 500ml','NLK-CIF','8934567001000',5,'chai',35000.00,24000.00,'active','Làm sạch gương kính','2025-12-29 17:08:00',NULL,NULL,'',''),(101,'Bánh quy bơ Danisa 454g','BQB-DAN','8934567001017',6,'hộp',90000.00,65000.00,'active','Bánh quy bơ hộp thiếc','2025-12-29 17:08:00',NULL,NULL,'',''),(102,'Bánh Custas kem trứng hộp 6 cái','BC-KEM6','8934567001024',6,'hộp',35000.00,24000.00,'active','Bánh mềm nhân kem trứng','2025-12-29 17:08:00',NULL,NULL,'',''),(103,'Kẹo cao su Doublemint','KCS-DM','8934567001031',6,'gói',10000.00,6000.00,'active','Kẹo cao su bạc hà','2025-12-29 17:08:00',NULL,NULL,'',''),(104,'Bánh Goute mè giòn 144g','B-GME','8934567001048',6,'gói',25000.00,17000.00,'active','Bánh mặn giòn','2025-12-29 17:08:00',NULL,NULL,'',''),(105,'Socola sữa Milo Cube','SC-MLC','8934567001055',6,'gói',50000.00,35000.00,'active','Socola viên ăn vặt','2025-12-29 17:08:00',NULL,NULL,'',''),(106,'Bánh AFC lúa mì 200g','B-AFC','8934567001062',6,'gói',28000.00,20000.00,'active','Bánh quy ăn kiêng','2025-12-29 17:08:00',NULL,NULL,'',''),(107,'Kẹo dẻo Haribo Gummy Bear','KD-HGB','8934567001079',6,'gói',30000.00,21000.00,'active','Kẹo dẻo hình gấu','2025-12-29 17:08:00',NULL,NULL,'',''),(108,'Bánh mì sandwich lát (tươi)','BMS-LAT','8934567001086',6,'gói',18000.00,12000.00,'active','Bánh mì tươi ăn sáng','2025-12-29 17:08:00',NULL,NULL,'',''),(109,'Thạch rau câu Zai Zai (hộp)','TRC-ZZ','8934567001093',6,'hộp',40000.00,28000.00,'active','Thạch rau câu nhiều vị','2025-12-29 17:08:00',NULL,NULL,'',''),(110,'Bánh quy Oreo nhân kem 133g','BQ-OREO','8934567001109',6,'gói',15000.00,10000.00,'active','Bánh quy sô cô la','2025-12-29 17:08:00',NULL,NULL,'',''),(111,'Rượu vang Đà Lạt Classic 750ml','RV-DL750','8934567001116',7,'chai',120000.00,85000.00,'active','Rượu vang đỏ phổ thông','2025-12-29 17:08:00',NULL,NULL,'',''),(112,'Bia Tiger Crystal chai 330ml (thùng 24)','BTC-24','8934567001123',7,'thùng',400000.00,300000.00,'active','Bia Crystal nhẹ','2025-12-29 17:08:00',NULL,NULL,'',''),(113,'Rượu Soju Chum Churum 360ml','RS-CC','8934567001130',7,'chai',65000.00,45000.00,'active','Rượu Soju Hàn Quốc','2025-12-29 17:08:00',NULL,NULL,'',''),(114,'Bia Huda lon 330ml','B-HDA','8934567001147',7,'lon',13000.00,9500.00,'active','Bia địa phương miền Trung','2025-12-29 17:08:00',NULL,NULL,'',''),(115,'Nước ngọt có gas 7Up lon 330ml','NG-7UP','8934567001154',7,'lon',10000.00,7500.00,'active','Nước ngọt chanh','2025-12-29 17:08:00',NULL,NULL,'',''),(116,'Bia Larue chai 450ml','BL-450','8934567001161',7,'chai',18000.00,13000.00,'active','Chai bia truyền thống','2025-12-29 17:08:00',NULL,NULL,'',''),(117,'Rượu Nếp Mới 500ml','RNM-500','8934567001178',7,'chai',50000.00,35000.00,'active','Rượu nếp truyền thống','2025-12-29 17:08:00',NULL,NULL,'',''),(118,'Bia Strongbow Vị Dâu lon','BS-DV','8934567001185',7,'lon',20000.00,15000.00,'active','Cider trái cây','2025-12-29 17:08:00',NULL,NULL,'',''),(119,'Bia Sapporo lon 330ml','BS-330','8934567001192',7,'lon',19000.00,14000.00,'active','Bia Nhật Bản','2025-12-29 17:08:00',NULL,NULL,'',''),(120,'Rượu Whisky Johnnie Walker Red Label 750ml','RW-JWRL','8934567001208',7,'chai',550000.00,400000.00,'active','Rượu mạnh','2025-12-29 17:08:00',NULL,NULL,'',''),(121,'Mì Tiến Vua Bò Hầm','M-TVBH','8934567001215',8,'gói',7000.00,4500.00,'active','Mì không chiên vị bò','2025-12-29 17:08:00',NULL,NULL,'',''),(122,'Hủ tiếu Nam Vang Gói','HT-NVG','8934567001222',8,'gói',15000.00,10000.00,'active','Hủ tiếu ăn liền','2025-12-29 17:08:00',NULL,NULL,'',''),(123,'Cháo Yến Mạch ăn liền 50g','CYM-50','8934567001239',8,'gói',9000.00,6000.00,'active','Cháo yến mạch ăn liền','2025-12-29 17:08:00',NULL,NULL,'',''),(124,'Mì 3 Miền Tôm Chua Cay','M3M-TCC','8934567001246',8,'gói',5500.00,3500.00,'active','Mì phổ thông vị tôm chua cay','2025-12-29 17:08:00',NULL,NULL,'',''),(125,'Miến Phú Hương Sườn Heo','MPH-SH','8934567001253',8,'gói',10000.00,7000.00,'active','Miến ăn liền vị sườn heo','2025-12-29 17:08:00',NULL,NULL,'',''),(126,'Mì lẩu thái Gấu Đỏ','MLT-GD','8934567001260',8,'gói',6500.00,4000.00,'active','Mì gói vị lẩu thái','2025-12-29 17:08:00',NULL,NULL,'',''),(127,'Bún riêu cua ăn liền','BRC-AL','8934567001277',8,'gói',13000.00,9000.00,'active','Bún ăn liền vị riêu cua','2025-12-29 17:08:00',NULL,NULL,'',''),(128,'Cháo đậu xanh gói','CDX-GOI','8934567001284',8,'gói',7000.00,4500.00,'active','Cháo dinh dưỡng đậu xanh','2025-12-29 17:08:00',NULL,NULL,'',''),(129,'Mì Reeva Lẩu nấm','M-RVLN','8934567001291',8,'gói',8000.00,5500.00,'active','Mì có sợi khoai tây','2025-12-29 17:08:00',NULL,NULL,'',''),(130,'Phở khô Sông Hậu 400g','PK-SH400','8934567001307',8,'gói',20000.00,14000.00,'active','Phở khô làm bún','2025-12-29 17:08:00',NULL,NULL,'',''),(131,'Thịt lợn hầm Tiên Yết 150g','TLH-TY','8934567001314',9,'hộp',35000.00,25000.00,'active','Thịt lợn hầm sẵn','2025-12-29 17:08:00',NULL,NULL,'',''),(132,'Cá Ngừ Ngâm Dầu hộp 185g','CN-ND','8934567001321',9,'hộp',60000.00,42000.00,'active','Cá ngừ đóng hộp','2025-12-29 17:08:00',NULL,NULL,'',''),(133,'Đậu cô ve muối chua 400g','DCVM-400','8934567001338',9,'hũ',25000.00,17000.00,'active','Rau củ ngâm chua','2025-12-29 17:08:00',NULL,NULL,'',''),(134,'Sữa Đặc Ngôi Sao Phương Nam 380g','SD-NSPN','8934567001345',9,'lon',26000.00,19000.00,'active','Sữa đặc có đường','2025-12-29 17:08:00',NULL,NULL,'',''),(135,'Lạp xưởng đóng gói 500g','LX-DG500','8934567001352',9,'gói',90000.00,65000.00,'active','Thực phẩm chế biến','2025-12-29 17:08:00',NULL,NULL,'',''),(136,'Nước cốt gà Vifon 180g','NCG-VF','8934567001369',9,'hộp',45000.00,30000.00,'active','Nước cốt hầm xương','2025-12-29 17:08:00',NULL,NULL,'',''),(137,'Thịt gà xé phay đóng hộp 150g','TGXP-150','8934567001376',9,'hộp',38000.00,26000.00,'active','Thịt gà đóng hộp ăn liền','2025-12-29 17:08:00',NULL,NULL,'',''),(138,'Bò kho đóng hộp 300g','BK-DH300','8934567001383',9,'hộp',55000.00,40000.00,'active','Bò kho chế biến sẵn','2025-12-29 17:08:00',NULL,NULL,'',''),(139,'Dứa (thơm) đóng hộp cắt lát 565g','DT-CLL','8934567001390',9,'lon',40000.00,28000.00,'active','Trái cây đóng hộp','2025-12-29 17:08:00',NULL,NULL,'',''),(140,'Đậu phụ chiên đóng hộp 200g','DPC-DH','8934567001406',9,'hộp',20000.00,13000.00,'active','Đậu phụ chiên sẵn','2025-12-29 17:08:00',NULL,NULL,'',''),(141,'Thuốc Lá Craven A Gói','TL-CA','8934567001413',10,'gói',32000.00,24000.00,'active','Thuốc lá','2025-12-29 17:08:00',NULL,NULL,'',''),(142,'Thuốc Lá Marlboro Lights Gói','TL-MLT','8934567001420',10,'gói',45000.00,33000.00,'active','Thuốc lá nhẹ','2025-12-29 17:08:00',NULL,NULL,'',''),(143,'Bật Lửa Đá Zippo (chưa đổ xăng)','BL-ZPO','8934567001437',10,'cái',350000.00,250000.00,'active','Bật lửa tái sử dụng','2025-12-29 17:08:00',NULL,NULL,'',''),(144,'Xăng Bật Lửa Zippo 125ml','XBL-ZPO','8934567001444',10,'chai',50000.00,35000.00,'active','Nhiên liệu bật lửa','2025-12-29 17:08:00',NULL,NULL,'',''),(145,'Giấy cuốn thuốc lá 100 tờ','GCT-100','8934567001451',10,'tệp',15000.00,10000.00,'active','Giấy cuốn thuốc','2025-12-29 17:08:00',NULL,NULL,'',''),(146,'Tẩu hút thuốc lá nhỏ','THTL-N','8934567001468',10,'cái',80000.00,55000.00,'active','Dụng cụ hút thuốc','2025-12-29 17:08:00',NULL,NULL,'',''),(147,'Thuốc Lá Thăng Long','TL-TL','8934567001475',10,'gói',25000.00,18000.00,'active','Thuốc lá phổ thông Việt Nam','2025-12-29 17:08:00',NULL,NULL,'',''),(148,'Diêm hộp lớn 100 que','D-HL','8934567001482',10,'hộp',3000.00,1500.00,'active','Diêm dùng nhiều lần','2025-12-29 17:08:00',NULL,NULL,'',''),(149,'Bật lửa điện sạc USB','BL-USB','8934567001499',10,'cái',120000.00,80000.00,'active','Bật lửa hiện đại','2025-12-29 17:08:00',NULL,NULL,'',''),(150,'Giấy quấn thuốc lá Zig-Zag','GQTL-ZZ','8934567001505',10,'gói',20000.00,13000.00,'active','Giấy cuộn thuốc lá','2025-12-29 17:08:00',NULL,NULL,'','');
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shelves`
--

DROP TABLE IF EXISTS `shelves`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shelves` (
  `shelf_id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint NOT NULL,
  `shelf_code` varchar(50) NOT NULL,
  `shelf_name` varchar(100) DEFAULT NULL,
  `status` enum('active','inactive') DEFAULT 'active',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`shelf_id`),
  KEY `fk_shelf_warehouse` (`warehouse_id`),
  CONSTRAINT `fk_shelf_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses` (`warehouse_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shelves`
--

LOCK TABLES `shelves` WRITE;
/*!40000 ALTER TABLE `shelves` DISABLE KEYS */;
/*!40000 ALTER TABLE `shelves` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(100) NOT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `role` varchar(20) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `branch_id` bigint DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `note` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `fk_user_branch` (`branch_id`),
  CONSTRAINT `FK9o70sp9ku40077y38fk4wieyk` FOREIGN KEY (`branch_id`) REFERENCES `branches` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','$2a$10$7gz3idM0iA0ikYyibDutqe31yrWDdVh2NIRa1gCj0QXVNw9723f0G','admin@bizflow.com','Administrator',NULL,'ADMIN',1,NULL,'2025-12-21 10:47:36','2025-12-21 10:47:36',NULL),(2,'owner','$2a$10$iDS5.CarVV4hxkD1P5oVYePzl/M8gs3jse7bGOAjhQBZ6iefSllWy','owner@bizflow.com','Store Owner',NULL,'OWNER',1,NULL,'2025-12-21 10:47:36','2025-12-21 10:47:36',NULL),(3,'test','$2a$10$iDS5.CarVV4hxkD1P5oVYePzl/M8gs3jse7bGOAjhQBZ6iefSllWy','test@bizflow.com','Test User',NULL,'EMPLOYEE',1,NULL,'2025-12-21 10:47:36','2025-12-21 10:47:36',NULL),(4,'vietphd','$2a$10$hTmAfVr7LjuSr5AxSKrpJeleoHtsiZn1RuVH9jub038t4C5SAIhiq','nhanvien1@gmail.com','viet','0902313141','EMPLOYEE',1,NULL,'2025-12-24 16:44:38','2025-12-24 16:44:38',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouses`
--

DROP TABLE IF EXISTS `warehouses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouses` (
  `warehouse_id` bigint NOT NULL AUTO_INCREMENT,
  `warehouse_name` varchar(255) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  `status` enum('active','inactive') DEFAULT 'active',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`warehouse_id`),
  KEY `fk_warehouse_manager` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouses`
--

LOCK TABLES `warehouses` WRITE;
/*!40000 ALTER TABLE `warehouses` DISABLE KEYS */;
/*!40000 ALTER TABLE `warehouses` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-29 18:41:22
