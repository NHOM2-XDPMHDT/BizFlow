-- Migration to reset DB and load SMALL schema (13 tables)
-- WARNING: This will drop ALL existing tables in bizflow_db.

CREATE DATABASE IF NOT EXISTS bizflow_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bizflow_db;

SET FOREIGN_KEY_CHECKS = 0;
-- Drop everything (covers both full and small variants)
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS stock_import_items;
DROP TABLE IF EXISTS stock_imports;
DROP TABLE IF EXISTS inventory_transactions;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS product_status_logs;
DROP TABLE IF EXISTS product_images;
DROP TABLE IF EXISTS product_prices;
DROP TABLE IF EXISTS product_units;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS product_categories;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS loyalty_points;
DROP TABLE IF EXISTS loyalty_transactions;
DROP TABLE IF EXISTS membership_tiers;
DROP TABLE IF EXISTS customer_debts;
DROP TABLE IF EXISTS receipts;
DROP TABLE IF EXISTS payment_logs;
DROP TABLE IF EXISTS purchase_order_items;
DROP TABLE IF EXISTS purchase_orders;
DROP TABLE IF EXISTS ai_suggestions;
DROP TABLE IF EXISTS product_sales_stats;
DROP TABLE IF EXISTS inventory_entries;
DROP TABLE IF EXISTS revenue_reports;
DROP TABLE IF EXISTS owner_subscriptions;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS shops;
DROP TABLE IF EXISTS branches;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
SET FOREIGN_KEY_CHECKS = 1;

-- Load small schema
SOURCE db/init/001_schema_small.sql;
-- Seed small data
SOURCE db/init/002_seed_small.sql;

-- Verify
SELECT 'SMALL migration completed' AS status;
SELECT COUNT(*) AS total_tables FROM information_schema.tables WHERE table_schema='bizflow_db';
