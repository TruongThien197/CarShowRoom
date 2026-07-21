package com.hsf302.carshowroom.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@org.springframework.core.annotation.Order(-100)
@RequiredArgsConstructor
public class SchemaMigrationRunner implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        migrateProductTable();
        migrateOrders();
        migrateBookings();
        migrateServices();
        migrateCartAndOrderItems();
        migrateProductCarModels();
        migrateCustomerVehicles();
        removeProductsTable();
    }

    private void migrateProductTable() {
        List<String> statements = List.of(
                "IF OBJECT_ID('product', 'U') IS NULL AND OBJECT_ID('products', 'U') IS NOT NULL " +
                        "CREATE TABLE product (" +
                        "product_id INT IDENTITY(1,1) PRIMARY KEY, " +
                        "category_id INT NOT NULL, " +
                        "product_name NVARCHAR(150) NOT NULL, " +
                        "sku VARCHAR(255) NULL, " +
                        "description NVARCHAR(MAX) NULL, " +
                        "price NUMERIC(18, 2) NOT NULL, " +
                        "stock_quantity INT NOT NULL DEFAULT 0, " +
                        "reserved_stock INT NULL, " +
                        "image_url NVARCHAR(500) NULL, " +
                        "status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE', " +
                        "version BIGINT NULL, " +
                        "created_at DATETIME2 NULL, " +
                        "updated_at DATETIME2 NULL)",
                "IF COL_LENGTH('product', 'sku') IS NULL ALTER TABLE product ADD sku VARCHAR(255) NULL",
                "IF COL_LENGTH('product', 'reserved_stock') IS NULL ALTER TABLE product ADD reserved_stock INT NULL",
                "IF COL_LENGTH('product', 'created_at') IS NULL ALTER TABLE product ADD created_at DATETIME2 NULL",
                "IF COL_LENGTH('product', 'updated_at') IS NULL ALTER TABLE product ADD updated_at DATETIME2 NULL",
                "IF COL_LENGTH('product', 'version') IS NULL ALTER TABLE product ADD version BIGINT NULL",
                "IF OBJECT_ID('products', 'U') IS NOT NULL " +
                        "UPDATE target SET " +
                        "target.product_name = source.name, " +
                        "target.sku = COALESCE(NULLIF(source.sku, ''), target.sku), " +
                        "target.description = COALESCE(source.description, target.description), " +
                        "target.price = source.price, " +
                        "target.stock_quantity = source.physical_stock, " +
                        "target.reserved_stock = source.reserved_stock, " +
                        "target.image_url = COALESCE(source.image_url, target.image_url), " +
                        "target.status = source.status, " +
                        "target.version = COALESCE(source.version, target.version), " +
                        "target.created_at = COALESCE(source.created_at, target.created_at), " +
                        "target.updated_at = COALESCE(source.updated_at, target.updated_at) " +
                        "FROM product target JOIN products source ON source.id = target.product_id",
                "IF OBJECT_ID('products', 'U') IS NOT NULL " +
                        "SET IDENTITY_INSERT product ON; " +
                        "IF OBJECT_ID('products', 'U') IS NOT NULL " +
                        "INSERT INTO product (product_id, category_id, product_name, sku, description, price, stock_quantity, reserved_stock, image_url, status, version, created_at, updated_at) " +
                        "SELECT source.id, source.category_id, source.name, COALESCE(NULLIF(source.sku, ''), CONCAT('PRODUCT-', source.id)), source.description, source.price, source.physical_stock, source.reserved_stock, source.image_url, source.status, source.version, source.created_at, source.updated_at " +
                        "FROM products source " +
                        "WHERE source.category_id IS NOT NULL " +
                        "AND NOT EXISTS (SELECT 1 FROM product target WHERE target.product_id = source.id); " +
                        "IF OBJECT_ID('products', 'U') IS NOT NULL SET IDENTITY_INSERT product OFF",
                "UPDATE product SET sku = CONCAT('LEGACY-', product_id) WHERE sku IS NULL OR LTRIM(RTRIM(sku)) = ''",
                "UPDATE product SET price = 0 WHERE price IS NULL",
                "UPDATE product SET reserved_stock = 0 WHERE reserved_stock IS NULL",
                "UPDATE product SET version = 0 WHERE version IS NULL",
                "UPDATE product SET created_at = SYSDATETIME() WHERE created_at IS NULL",
                "UPDATE product SET updated_at = created_at WHERE updated_at IS NULL",
                "DECLARE @skuIndexSql NVARCHAR(MAX) = N''; " +
                        "SELECT @skuIndexSql = @skuIndexSql + N'DROP INDEX [' + i.name + N'] ON product;' " +
                        "FROM sys.indexes i " +
                        "JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id " +
                        "JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id " +
                        "WHERE i.object_id = OBJECT_ID('product') AND i.is_unique = 1 AND c.name = 'sku' AND i.is_primary_key = 0; " +
                        "IF LEN(@skuIndexSql) > 0 EXEC sp_executesql @skuIndexSql",
                "IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'product' AND COLUMN_NAME = 'sku' AND IS_NULLABLE = 'YES') ALTER TABLE product ALTER COLUMN sku VARCHAR(255) NOT NULL",
                "IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'product' AND COLUMN_NAME = 'price' AND IS_NULLABLE = 'YES') ALTER TABLE product ALTER COLUMN price NUMERIC(18, 2) NOT NULL",
                "IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'product' AND COLUMN_NAME = 'reserved_stock' AND IS_NULLABLE = 'YES') ALTER TABLE product ALTER COLUMN reserved_stock INT NOT NULL",
                "IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'product' AND COLUMN_NAME = 'version' AND IS_NULLABLE = 'YES') ALTER TABLE product ALTER COLUMN version BIGINT NOT NULL",
                "IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'product' AND COLUMN_NAME = 'created_at' AND IS_NULLABLE = 'YES') ALTER TABLE product ALTER COLUMN created_at DATETIME2 NOT NULL",
                "IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'product' AND COLUMN_NAME = 'updated_at' AND IS_NULLABLE = 'YES') ALTER TABLE product ALTER COLUMN updated_at DATETIME2 NOT NULL",
                "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('product') AND name = 'UX_product_sku') CREATE UNIQUE INDEX UX_product_sku ON product(sku)"
        );
        statements.forEach(statement -> {
            try {
                jdbcTemplate.execute(statement);
            } catch (RuntimeException ignored) {
                // Keep startup tolerant for older local demo databases while later statements finish cleanup.
            }
        });
    }

    private void migrateOrders() {
        List<String> statements = List.of(
                "IF COL_LENGTH('orders', 'created_at') IS NULL ALTER TABLE orders ADD created_at DATETIME2 NULL",
                "IF COL_LENGTH('orders', 'updated_at') IS NULL ALTER TABLE orders ADD updated_at DATETIME2 NULL",
                "IF COL_LENGTH('orders', 'order_type') IS NULL ALTER TABLE orders ADD order_type VARCHAR(255) NULL",
                "IF COL_LENGTH('orders', 'order_status') IS NULL ALTER TABLE orders ADD order_status VARCHAR(255) NULL",
                "IF COL_LENGTH('orders', 'payment_status') IS NULL ALTER TABLE orders ADD payment_status VARCHAR(255) NULL",
                "IF COL_LENGTH('orders', 'payment_method') IS NULL ALTER TABLE orders ADD payment_method VARCHAR(20) NULL",
                "IF COL_LENGTH('orders', 'product_total') IS NULL ALTER TABLE orders ADD product_total NUMERIC(18, 2) NULL",
                "IF COL_LENGTH('orders', 'receiver_phone') IS NULL ALTER TABLE orders ADD receiver_phone NVARCHAR(30) NULL",
                "IF COL_LENGTH('orders', 'cancellation_reason') IS NULL ALTER TABLE orders ADD cancellation_reason NVARCHAR(500) NULL",
                "IF COL_LENGTH('orders', 'shipping_carrier') IS NULL ALTER TABLE orders ADD shipping_carrier NVARCHAR(100) NULL",
                "IF COL_LENGTH('orders', 'tracking_code') IS NULL ALTER TABLE orders ADD tracking_code NVARCHAR(100) NULL",
                "IF COL_LENGTH('orders', 'refund_status') IS NULL ALTER TABLE orders ADD refund_status VARCHAR(30) NULL",
                "IF COL_LENGTH('orders', 'refund_note') IS NULL ALTER TABLE orders ADD refund_note NVARCHAR(500) NULL",
                "IF COL_LENGTH('orders', 'refunded_at') IS NULL ALTER TABLE orders ADD refunded_at DATETIME2 NULL",
                "IF COL_LENGTH('orders', 'refunded_by_id') IS NULL ALTER TABLE orders ADD refunded_by_id INT NULL",
                "UPDATE orders SET refund_status = 'NONE' WHERE refund_status IS NULL",
                "IF COL_LENGTH('orders', 'order_date') IS NOT NULL EXEC sp_executesql N'UPDATE orders SET created_at = CAST(order_date AS DATETIME2) WHERE created_at IS NULL AND order_date IS NOT NULL'",
                "UPDATE orders SET updated_at = created_at WHERE updated_at IS NULL",
                "UPDATE orders SET order_type = 'SHIPPING' WHERE order_type IS NULL",
                "IF COL_LENGTH('orders', 'status') IS NOT NULL EXEC sp_executesql N'UPDATE orders SET order_status = CASE UPPER(status) " +
                        "WHEN ''PENDING'' THEN ''PENDING_PAYMENT'' " +
                        "WHEN ''SHIPPED'' THEN ''SHIPPING'' " +
                        "WHEN ''DELIVERED'' THEN ''COMPLETED'' " +
                        "WHEN ''CANCELLED'' THEN ''CANCELED'' " +
                        "ELSE UPPER(status) END WHERE order_status IS NULL AND status IS NOT NULL'",
                "UPDATE orders SET order_status = 'PENDING_PAYMENT' WHERE order_status = 'PENDING_DEPOSIT'",
                "UPDATE orders SET order_status = 'PROCESSING' WHERE order_status = 'DEPOSITED'",
                "UPDATE orders SET payment_status = CASE WHEN order_status IN ('COMPLETED', 'PROCESSING', 'SHIPPING') THEN 'PAID' ELSE 'PENDING' END WHERE payment_status IS NULL",
                "UPDATE orders SET payment_method = 'PAYOS' WHERE payment_method IS NULL",
                "IF COL_LENGTH('orders', 'total_amount') IS NOT NULL EXEC sp_executesql N'UPDATE orders SET product_total = total_amount WHERE product_total IS NULL'",
                "IF COL_LENGTH('orders', 'deposit_amount') IS NOT NULL ALTER TABLE orders DROP COLUMN deposit_amount",
                "IF COL_LENGTH('orders', 'remaining_amount') IS NOT NULL ALTER TABLE orders DROP COLUMN remaining_amount"
        );
        statements.forEach(jdbcTemplate::execute);
    }

    private void migrateBookings() {
        List<String> statements = List.of(
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'deposit_amount') IS NULL ALTER TABLE bookings ADD deposit_amount NUMERIC(18, 2) NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'refund_status') IS NULL ALTER TABLE bookings ADD refund_status VARCHAR(30) NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'remaining_payment_status') IS NULL ALTER TABLE bookings ADD remaining_payment_status VARCHAR(30) NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'checked_in_at') IS NULL ALTER TABLE bookings ADD checked_in_at DATETIME2 NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'no_show_at') IS NULL ALTER TABLE bookings ADD no_show_at DATETIME2 NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'refund_note') IS NULL ALTER TABLE bookings ADD refund_note NVARCHAR(500) NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'refund_bank_name') IS NULL ALTER TABLE bookings ADD refund_bank_name NVARCHAR(100) NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'refund_account_holder') IS NULL ALTER TABLE bookings ADD refund_account_holder NVARCHAR(150) NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'refund_account_number') IS NULL ALTER TABLE bookings ADD refund_account_number NVARCHAR(50) NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'refunded_at') IS NULL ALTER TABLE bookings ADD refunded_at DATETIME2 NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'refunded_by_id') IS NULL ALTER TABLE bookings ADD refunded_by_id INT NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'refund_status') IS NOT NULL UPDATE bookings SET refund_status = 'NONE' WHERE refund_status IS NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'remaining_payment_status') IS NOT NULL UPDATE bookings SET remaining_payment_status = 'PENDING' WHERE remaining_payment_status IS NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'booking_status') IS NOT NULL UPDATE bookings SET booking_status = 'PENDING_PAYMENT' WHERE booking_status = 'PENDING_DEPOSIT'",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'deposit_amount') IS NOT NULL " +
                        "EXEC sp_executesql N'UPDATE bookings SET final_amount = COALESCE(final_amount, estimated_min_amount, deposit_amount)'",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'deposit_amount') IS NOT NULL " +
                        "UPDATE bookings SET deposit_amount = CASE " +
                        "WHEN estimated_min_amount IS NULL OR estimated_min_amount * 0.20 < 2000 THEN 2000 " +
                        "WHEN estimated_min_amount * 0.20 > 10000 THEN 10000 " +
                        "ELSE CEILING(estimated_min_amount * 0.20) END " +
                        "WHERE deposit_amount IS NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'remaining_amount') IS NOT NULL AND COL_LENGTH('bookings', 'final_amount') IS NOT NULL " +
                        "EXEC sp_executesql N'UPDATE bookings SET final_amount = COALESCE(final_amount, estimated_min_amount) WHERE final_amount IS NULL'",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND COL_LENGTH('bookings', 'remaining_amount') IS NOT NULL ALTER TABLE bookings DROP COLUMN remaining_amount"
        );
        statements.forEach(jdbcTemplate::execute);
    }

    private void migrateServices() {
        List<String> statements = List.of(
                "IF COL_LENGTH('service', 'min_price') IS NULL ALTER TABLE service ADD min_price NUMERIC(18, 2) NULL",
                "IF COL_LENGTH('service', 'max_price') IS NULL ALTER TABLE service ADD max_price NUMERIC(18, 2) NULL",
                "IF COL_LENGTH('service', 'duration_minutes') IS NULL ALTER TABLE service ADD duration_minutes INT NULL",
                "IF COL_LENGTH('service', 'status') IS NULL ALTER TABLE service ADD status VARCHAR(255) NULL",
                "IF COL_LENGTH('service', 'price') IS NOT NULL AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'service' AND COLUMN_NAME = 'price' AND IS_NULLABLE = 'NO') ALTER TABLE service ALTER COLUMN price NUMERIC(18, 2) NULL",
                "IF COL_LENGTH('service', 'price') IS NOT NULL EXEC sp_executesql N'UPDATE service SET min_price = price WHERE min_price IS NULL AND price IS NOT NULL'",
                "IF COL_LENGTH('service', 'price') IS NOT NULL EXEC sp_executesql N'UPDATE service SET max_price = price WHERE max_price IS NULL AND price IS NOT NULL'",
                "UPDATE service SET duration_minutes = 60 WHERE duration_minutes IS NULL",
                "UPDATE service SET status = 'ACTIVE' WHERE status IS NULL",
                "UPDATE service SET min_price = 0 WHERE min_price IS NULL",
                "UPDATE service SET max_price = min_price WHERE max_price IS NULL",
                "UPDATE service SET service_name = CASE " +
                        "WHEN duration_minutes = 30 AND min_price = 20000 THEN N'Chẩn đoán kỹ thuật số' " +
                        "WHEN duration_minutes = 60 AND min_price = 35000 THEN N'Kiểm tra phanh và gầm xe' " +
                        "WHEN duration_minutes = 90 AND min_price = 40000 THEN N'Tinh chỉnh hiệu suất động cơ' " +
                        "WHEN duration_minutes = 45 AND min_price = 30000 THEN N'Bảo dưỡng định kỳ' " +
                        "ELSE service_name END",
                "IF OBJECT_ID('booking_services', 'U') IS NOT NULL AND OBJECT_ID('service', 'U') IS NOT NULL " +
                        "UPDATE bs SET service_name_snapshot = s.service_name FROM booking_services bs JOIN service s ON bs.service_id = s.service_id",
                "IF NOT EXISTS (SELECT 1 FROM service WHERE min_price IS NULL) AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'service' AND COLUMN_NAME = 'min_price' AND IS_NULLABLE = 'YES') ALTER TABLE service ALTER COLUMN min_price NUMERIC(18, 2) NOT NULL",
                "IF NOT EXISTS (SELECT 1 FROM service WHERE max_price IS NULL) AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'service' AND COLUMN_NAME = 'max_price' AND IS_NULLABLE = 'YES') ALTER TABLE service ALTER COLUMN max_price NUMERIC(18, 2) NOT NULL",
                "IF NOT EXISTS (SELECT 1 FROM service WHERE duration_minutes IS NULL) AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'service' AND COLUMN_NAME = 'duration_minutes' AND IS_NULLABLE = 'YES') ALTER TABLE service ALTER COLUMN duration_minutes INT NOT NULL",
                "IF NOT EXISTS (SELECT 1 FROM service WHERE status IS NULL) AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'service' AND COLUMN_NAME = 'status' AND IS_NULLABLE = 'YES') ALTER TABLE service ALTER COLUMN status VARCHAR(255) NOT NULL"
        );
        statements.forEach(jdbcTemplate::execute);
    }

    private void migrateCartAndOrderItems() {
        List<String> statements = List.of(
                "IF COL_LENGTH('cart_item', 'fulfillment_type') IS NULL ALTER TABLE cart_item ADD fulfillment_type VARCHAR(255) NULL",
                "UPDATE cart_item SET fulfillment_type = 'SHIPPING' WHERE fulfillment_type IS NULL",
                "IF OBJECT_ID('order_items', 'U') IS NOT NULL AND COL_LENGTH('order_items', 'product_name_snapshot') IS NULL ALTER TABLE order_items ADD product_name_snapshot VARCHAR(255) NULL",
                "IF OBJECT_ID('order_items', 'U') IS NOT NULL UPDATE oi SET product_name_snapshot = p.product_name FROM order_items oi JOIN product p ON oi.product_id = p.product_id WHERE oi.product_name_snapshot IS NULL",
                "IF COL_LENGTH('orders', 'parent_order_id') IS NOT NULL AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'orders' AND COLUMN_NAME = 'parent_order_id' AND DATA_TYPE = 'bigint') ALTER TABLE orders ALTER COLUMN parent_order_id INT NULL",
                "IF OBJECT_ID('order_items', 'U') IS NOT NULL AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'order_items' AND COLUMN_NAME = 'order_id' AND DATA_TYPE = 'bigint') ALTER TABLE order_items ALTER COLUMN order_id INT NOT NULL",
                "IF OBJECT_ID('payment_transactions', 'U') IS NOT NULL AND COL_LENGTH('payment_transactions', 'payment_purpose') IS NULL ALTER TABLE payment_transactions ADD payment_purpose VARCHAR(30) NULL",
                "IF OBJECT_ID('payment_transactions', 'U') IS NOT NULL AND COL_LENGTH('payment_transactions', 'payment_purpose') IS NOT NULL UPDATE payment_transactions SET payment_purpose = 'DEPOSIT' WHERE payment_purpose IS NULL",
                "IF OBJECT_ID('payment_transactions', 'U') IS NOT NULL AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'payment_transactions' AND COLUMN_NAME = 'order_id' AND DATA_TYPE = 'bigint') ALTER TABLE payment_transactions ALTER COLUMN order_id INT NULL",
                "IF OBJECT_ID('payment_transactions', 'U') IS NOT NULL AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'payment_transactions' AND COLUMN_NAME = 'parent_order_id' AND DATA_TYPE = 'bigint') ALTER TABLE payment_transactions ALTER COLUMN parent_order_id INT NULL",
                "IF OBJECT_ID('inventory_reservations', 'U') IS NOT NULL AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'inventory_reservations' AND COLUMN_NAME = 'order_id' AND DATA_TYPE = 'bigint') ALTER TABLE inventory_reservations ALTER COLUMN order_id INT NULL",
                "IF OBJECT_ID('bookings', 'U') IS NOT NULL AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'bookings' AND COLUMN_NAME = 'related_order_id' AND DATA_TYPE = 'bigint') ALTER TABLE bookings ALTER COLUMN related_order_id INT NULL"
        );
        statements.forEach(statement -> {
            try {
                jdbcTemplate.execute(statement);
            } catch (RuntimeException ignored) {
                // Existing demo databases may have incompatible foreign keys from earlier scaffolds.
                // Hibernate can still run with nullable integer ids; avoid blocking startup.
            }
        });
    }

    private void migrateProductCarModels() {
        List<String> statements = List.of(
                "IF OBJECT_ID('product_car_models', 'U') IS NULL CREATE TABLE product_car_models (product_id INT NOT NULL, car_model_id INT NOT NULL, CONSTRAINT pk_product_car_models PRIMARY KEY (product_id, car_model_id))",
                "IF OBJECT_ID('fk_product_car_models_product', 'F') IS NULL AND OBJECT_ID('product', 'U') IS NOT NULL ALTER TABLE product_car_models ADD CONSTRAINT fk_product_car_models_product FOREIGN KEY (product_id) REFERENCES product(product_id)",
                "IF OBJECT_ID('fk_product_car_models_car_model', 'F') IS NULL AND OBJECT_ID('car_model', 'U') IS NOT NULL ALTER TABLE product_car_models ADD CONSTRAINT fk_product_car_models_car_model FOREIGN KEY (car_model_id) REFERENCES car_model(car_model_id)"
        );
        statements.forEach(statement -> {
            try {
                jdbcTemplate.execute(statement);
            } catch (RuntimeException ignored) {
                // Keep startup tolerant for older local demo databases with partial schema changes.
            }
        });
    }

    private void migrateCustomerVehicles() {
        List<String> statements = List.of(
                "IF COL_LENGTH('vehicle', 'brand') IS NULL ALTER TABLE vehicle ADD brand NVARCHAR(100) NULL",
                "IF COL_LENGTH('vehicle', 'model_name') IS NULL ALTER TABLE vehicle ADD model_name NVARCHAR(100) NULL",
                "IF COL_LENGTH('vehicle', 'year') IS NULL ALTER TABLE vehicle ADD [year] INT NULL",
                "IF COL_LENGTH('vehicle', 'brand') IS NOT NULL UPDATE v SET brand = COALESCE(v.brand, cm.brand) FROM vehicle v LEFT JOIN car_model cm ON v.car_model_id = cm.car_model_id",
                "IF COL_LENGTH('vehicle', 'model_name') IS NOT NULL UPDATE v SET model_name = COALESCE(v.model_name, cm.model_name) FROM vehicle v LEFT JOIN car_model cm ON v.car_model_id = cm.car_model_id",
                "IF COL_LENGTH('vehicle', 'year') IS NOT NULL UPDATE v SET [year] = COALESCE(v.[year], cm.[year]) FROM vehicle v LEFT JOIN car_model cm ON v.car_model_id = cm.car_model_id",
                "IF COL_LENGTH('vehicle', 'car_model_id') IS NOT NULL UPDATE v SET car_model_id = cm.car_model_id FROM vehicle v JOIN car_model cm ON LOWER(v.brand) = LOWER(cm.brand) AND LOWER(v.model_name) = LOWER(cm.model_name) AND v.[year] = cm.[year] WHERE v.car_model_id IS NULL",
                "IF COL_LENGTH('vehicle', 'brand') IS NOT NULL UPDATE vehicle SET brand = 'Unknown' WHERE brand IS NULL",
                "IF COL_LENGTH('vehicle', 'model_name') IS NOT NULL UPDATE vehicle SET model_name = 'Unknown' WHERE model_name IS NULL",
                "IF COL_LENGTH('vehicle', 'year') IS NOT NULL UPDATE vehicle SET [year] = YEAR(GETDATE()) WHERE [year] IS NULL",
                "IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'vehicle' AND COLUMN_NAME = 'car_model_id' AND IS_NULLABLE = 'NO') ALTER TABLE vehicle ALTER COLUMN car_model_id INT NULL"
        );
        statements.forEach(statement -> {
            try {
                jdbcTemplate.execute(statement);
            } catch (RuntimeException ignored) {
                // Keep local startup tolerant if SQL Server blocks altering an older FK column.
            }
        });
    }

    private void removeProductsTable() {
        List<String> statements = List.of(
                "DECLARE @sql NVARCHAR(MAX) = N''; " +
                        "SELECT @sql = @sql + N'ALTER TABLE [' + OBJECT_SCHEMA_NAME(parent_object_id) + N'].[' + OBJECT_NAME(parent_object_id) + N'] DROP CONSTRAINT [' + name + N'];' " +
                        "FROM sys.foreign_keys WHERE referenced_object_id = OBJECT_ID('products') OR parent_object_id = OBJECT_ID('products'); " +
                        "IF LEN(@sql) > 0 EXEC sp_executesql @sql",
                productForeignKeyStatement("cart_item", "FK_cart_item_product"),
                productForeignKeyStatement("order_items", "FK_order_items_product"),
                productForeignKeyStatement("inventory_reservations", "FK_inventory_reservations_product"),
                productForeignKeyStatement("booking_extra_items", "FK_booking_extra_items_product"),
                productForeignKeyStatement("product_car_models", "FK_product_car_models_product"),
                productForeignKeyStatement("product_compatibility", "FK_product_compatibility_product"),
                productForeignKeyStatement("order_detail", "FK_order_detail_product"),
                "DECLARE @duplicateFkSql NVARCHAR(MAX) = N''; " +
                        "WITH duplicate_fks AS (" +
                        "SELECT fk.name, fk.parent_object_id, " +
                        "ROW_NUMBER() OVER (PARTITION BY fk.parent_object_id, fkc.parent_column_id, fk.referenced_object_id, fkc.referenced_column_id ORDER BY fk.name) AS rn " +
                        "FROM sys.foreign_keys fk " +
                        "JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id " +
                        "WHERE fk.referenced_object_id = OBJECT_ID('product') " +
                        "AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'product_id') " +
                        "SELECT @duplicateFkSql = @duplicateFkSql + N'ALTER TABLE [' + OBJECT_SCHEMA_NAME(parent_object_id) + N'].[' + OBJECT_NAME(parent_object_id) + N'] DROP CONSTRAINT [' + name + N'];' " +
                        "FROM duplicate_fks WHERE rn > 1; " +
                        "IF LEN(@duplicateFkSql) > 0 EXEC sp_executesql @duplicateFkSql",
                "IF OBJECT_ID('products', 'U') IS NOT NULL DROP TABLE products"
        );
        statements.forEach(statement -> {
            try {
                jdbcTemplate.execute(statement);
            } catch (RuntimeException ignored) {
                // If a local database still has orphaned legacy rows, keep startup alive for manual cleanup.
            }
        });
    }

    private String productForeignKeyStatement(String tableName, String constraintName) {
        return "IF OBJECT_ID('" + tableName + "', 'U') IS NOT NULL " +
                "AND NOT EXISTS (" +
                "SELECT 1 FROM sys.foreign_keys fk " +
                "JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id " +
                "WHERE fk.parent_object_id = OBJECT_ID('" + tableName + "') " +
                "AND fk.referenced_object_id = OBJECT_ID('product') " +
                "AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'product_id') " +
                "ALTER TABLE " + tableName + " ADD CONSTRAINT " + constraintName + " FOREIGN KEY (product_id) REFERENCES product(product_id)";
    }

}
