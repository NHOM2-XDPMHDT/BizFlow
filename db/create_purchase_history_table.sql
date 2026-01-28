-- Create customer_purchase_history table for storing Kafka events
-- This script should be run against bizflow_db

CREATE TABLE IF NOT EXISTS `customer_purchase_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `customer_id` BIGINT NOT NULL,
  `order_id` BIGINT NOT NULL UNIQUE,
  `invoice_number` VARCHAR(30),
  `total_amount` DECIMAL(15,2),
  `status` VARCHAR(30),
  `order_created_at` DATETIME,
  `payment_method` VARCHAR(50),
  `order_items_json` LONGTEXT,
  `received_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_order_id` (`order_id`),
  KEY `FK_customer_id` (`customer_id`),
  KEY `IDX_customer_created` (`customer_id`, `order_created_at` DESC),
  CONSTRAINT `FK_customer_purchase_history_customer` 
    FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='Stores customer purchase history received from Kafka events';

-- Create index for faster queries on customer and date range
-- Note: Omitted as index IDX_customer_created already created above
