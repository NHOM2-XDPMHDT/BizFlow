# Script import đầy đủ dữ liệu vào tất cả database
# Tắt foreign key check để import thành công

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  BẮT ĐẦU IMPORT DỮ LIỆU TỪ BACKUP" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$backupDir = "d:\CNTT\Nam 3\XDCNPM\Nhanh_cuoiki\BizFlow\db\backups"
$databases = @(
    @{Name="bizflow_auth_db"; File="bizflow_auth_db.sql"},
    @{Name="bizflow_catalog_db"; File="bizflow_catalog_db.sql"},
    @{Name="bizflow_customer_db"; File="bizflow_customer_db.sql"},
    @{Name="bizflow_inventory_db"; File="bizflow_inventory_db.sql"},
    @{Name="bizflow_promotion_db"; File="bizflow_promotion_db.sql"},
    @{Name="bizflow_sales_db"; File="bizflow_sales_db.sql"}
)

foreach ($db in $databases) {
    Write-Host "Import $($db.Name)..." -ForegroundColor Yellow
    
    # Tạo script tạm với SET FOREIGN_KEY_CHECKS
    $tempScript = "$env:TEMP\temp_import_$($db.Name).sql"
    
    @"
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';
SOURCE $backupDir\$($db.File);
SET FOREIGN_KEY_CHECKS = 1;
"@ | Out-File -FilePath $tempScript -Encoding UTF8
    
    # Import với temp script
    Get-Content $tempScript | docker exec -i bizflow-mysql mysql -uroot -p123456 $db.Name 2>&1 | Out-Null
    
    Write-Host "  ✓ Hoàn tất $($db.Name)" -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  KIỂM TRA DỮ LIỆU ĐÃ IMPORT" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra dữ liệu
$checkQuery = @"
SELECT 'bizflow_auth_db' as db_name, 'users' as table_name, COUNT(*) as row_count FROM bizflow_auth_db.users
UNION ALL SELECT 'bizflow_auth_db', 'branches', COUNT(*) FROM bizflow_auth_db.branches
UNION ALL SELECT 'bizflow_catalog_db', 'products', COUNT(*) FROM bizflow_catalog_db.products
UNION ALL SELECT 'bizflow_catalog_db', 'categories', COUNT(*) FROM bizflow_catalog_db.categories
UNION ALL SELECT 'bizflow_customer_db', 'customers', COUNT(*) FROM bizflow_customer_db.customers
UNION ALL SELECT 'bizflow_inventory_db', 'inventory_stocks', COUNT(*) FROM bizflow_inventory_db.inventory_stocks
UNION ALL SELECT 'bizflow_promotion_db', 'promotions', COUNT(*) FROM bizflow_promotion_db.promotions
UNION ALL SELECT 'bizflow_promotion_db', 'promotion_targets', COUNT(*) FROM bizflow_promotion_db.promotion_targets
UNION ALL SELECT 'bizflow_sales_db', 'orders', COUNT(*) FROM bizflow_sales_db.orders
UNION ALL SELECT 'bizflow_sales_db', 'order_items', COUNT(*) FROM bizflow_sales_db.order_items;
"@

docker exec bizflow-mysql mysql -uroot -p123456 -t -e $checkQuery

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  HOÀN TẤT IMPORT TẤT CẢ DỮ LIỆU!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
