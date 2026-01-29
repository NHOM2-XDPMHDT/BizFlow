# Script import TẤT CẢ dữ liệu từ backups
# Extract INSERT statements và import vào database

$ErrorActionPreference = "Continue"
$backupDir = "d:\CNTT\Nam 3\XDCNPM\Nhanh_cuoiki\BizFlow\db\backups"

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "  IMPORT TẤT CẢ DỮ LIỆU TỪ BACKUP FILES" -ForegroundColor Cyan
Write-Host "============================================`n" -ForegroundColor Cyan

function Import-Database {
    param(
        [string]$DbName,
        [string]$BackupFile
    )
    
    Write-Host "Importing $DbName..." -ForegroundColor Yellow
    
    # Đọc file backup và extract INSERT statements
    $content = Get-Content "$backupDir\$BackupFile" -Raw -Encoding UTF8
    
    # Tạo SQL script tạm với REPLACE thay vì INSERT để tránh duplicate
    $insertStatements = $content -replace 'INSERT INTO', 'REPLACE INTO'
    
    # Tạo temp file
    $tempFile = "$env:TEMP\temp_$DbName.sql"
    
    @"
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';
USE $DbName;
"@ + $insertStatements + @"

SET FOREIGN_KEY_CHECKS = 1;
"@ | Out-File -FilePath $tempFile -Encoding UTF8
    
    # Import vào database
    Get-Content $tempFile | docker exec -i bizflow-mysql mysql -uroot -p123456 2>&1 | Out-Null
    
    # Cleanup
    Remove-Item $tempFile -ErrorAction SilentlyContinue
    
    Write-Host "  ✓ $DbName imported" -ForegroundColor Green
}

# Import từng database
Import-Database -DbName "bizflow_catalog_db" -BackupFile "bizflow_catalog_db.sql"
Import-Database -DbName "bizflow_inventory_db" -BackupFile "bizflow_inventory_db.sql"
Import-Database -DbName "bizflow_promotion_db" -BackupFile "bizflow_promotion_db.sql"
Import-Database -DbName "bizflow_sales_db" -BackupFile "bizflow_sales_db.sql"

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "  KIỂM TRA DỮ LIỆU" -ForegroundColor Cyan
Write-Host "============================================`n" -ForegroundColor Cyan

# Kiểm tra kết quả
docker exec bizflow-mysql mysql -uroot -p123456 -t -e @"
SELECT 'AUTH' as DB, 'users' as Table, COUNT(*) as Rows FROM bizflow_auth_db.users
UNION ALL SELECT 'AUTH', 'branches', COUNT(*) FROM bizflow_auth_db.branches
UNION ALL SELECT 'CATALOG', 'products', COUNT(*) FROM bizflow_catalog_db.products
UNION ALL SELECT 'CATALOG', 'categories', COUNT(*) FROM bizflow_catalog_db.categories
UNION ALL SELECT 'CUSTOMER', 'customers', COUNT(*) FROM bizflow_customer_db.customers
UNION ALL SELECT 'INVENTORY', 'inventory_stocks', COUNT(*) FROM bizflow_inventory_db.inventory_stocks
UNION ALL SELECT 'PROMOTION', 'promotions', COUNT(*) FROM bizflow_promotion_db.promotions
UNION ALL SELECT 'SALES', 'orders', COUNT(*) FROM bizflow_sales_db.orders
UNION ALL SELECT 'SALES', 'order_items', COUNT(*) FROM bizflow_sales_db.order_items;
"@ 2>&1 | Where-Object { $_ -notmatch "Warning|insecure" }

Write-Host "`n============================================" -ForegroundColor Green
Write-Host "  HOÀN TẤT!" -ForegroundColor Green  
Write-Host "============================================`n" -ForegroundColor Green
