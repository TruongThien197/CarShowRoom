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
        migrateOrders();
        migrateServices();
        migrateCartAndOrderItems();
        migrateProductCarModels();
        migrateCustomerVehicles();
    }

    private void migrateOrders() {
        List<String> statements = List.of(
                "IF COL_LENGTH('orders', 'created_at') IS NULL ALTER TABLE orders ADD created_at DATETIME2 NULL",
                "IF COL_LENGTH('orders', 'updated_at') IS NULL ALTER TABLE orders ADD updated_at DATETIME2 NULL",
                "IF COL_LENGTH('orders', 'order_type') IS NULL ALTER TABLE orders ADD order_type VARCHAR(255) NULL",
                "IF COL_LENGTH('orders', 'order_status') IS NULL ALTER TABLE orders ADD order_status VARCHAR(255) NULL",
                "IF COL_LENGTH('orders', 'payment_status') IS NULL ALTER TABLE orders ADD payment_status VARCHAR(255) NULL",
                "IF COL_LENGTH('orders', 'product_total') IS NULL ALTER TABLE orders ADD product_total NUMERIC(18, 2) NULL",
                "IF COL_LENGTH('orders', 'deposit_amount') IS NULL ALTER TABLE orders ADD deposit_amount NUMERIC(18, 2) NULL",
                "IF COL_LENGTH('orders', 'remaining_amount') IS NULL ALTER TABLE orders ADD remaining_amount NUMERIC(18, 2) NULL",
                "UPDATE orders SET created_at = CAST(order_date AS DATETIME2) WHERE created_at IS NULL AND order_date IS NOT NULL",
                "UPDATE orders SET updated_at = created_at WHERE updated_at IS NULL",
                "UPDATE orders SET order_type = 'SHIPPING' WHERE order_type IS NULL",
                "UPDATE orders SET order_status = CASE UPPER(status) " +
                        "WHEN 'PENDING' THEN 'PENDING_DEPOSIT' " +
                        "WHEN 'SHIPPED' THEN 'SHIPPING' " +
                        "WHEN 'DELIVERED' THEN 'COMPLETED' " +
                        "WHEN 'CANCELLED' THEN 'CANCELED' " +
                        "ELSE UPPER(status) END WHERE order_status IS NULL AND status IS NOT NULL",
                "UPDATE orders SET payment_status = CASE WHEN order_status IN ('COMPLETED', 'PROCESSING', 'SHIPPING') THEN 'PAID' ELSE 'PENDING' END WHERE payment_status IS NULL",
                "UPDATE orders SET product_total = total_amount WHERE product_total IS NULL",
                "UPDATE orders SET deposit_amount = ROUND(product_total * 0.2, 0) WHERE deposit_amount IS NULL",
                "UPDATE orders SET remaining_amount = product_total - deposit_amount WHERE remaining_amount IS NULL"
        );
        statements.forEach(jdbcTemplate::execute);
    }

    private void migrateServices() {
        List<String> statements = List.of(
                "IF COL_LENGTH('service', 'min_price') IS NULL ALTER TABLE service ADD min_price NUMERIC(18, 2) NULL",
                "IF COL_LENGTH('service', 'max_price') IS NULL ALTER TABLE service ADD max_price NUMERIC(18, 2) NULL",
                "IF COL_LENGTH('service', 'duration_minutes') IS NULL ALTER TABLE service ADD duration_minutes INT NULL",
                "IF COL_LENGTH('service', 'status') IS NULL ALTER TABLE service ADD status VARCHAR(255) NULL",
                "UPDATE service SET min_price = price WHERE min_price IS NULL AND price IS NOT NULL",
                "UPDATE service SET max_price = price WHERE max_price IS NULL AND price IS NOT NULL",
                "UPDATE service SET duration_minutes = 60 WHERE duration_minutes IS NULL",
                "UPDATE service SET status = 'ACTIVE' WHERE status IS NULL"
        );
        statements.forEach(jdbcTemplate::execute);
    }

    private void migrateCartAndOrderItems() {
        List<String> statements = List.of(
                "IF COL_LENGTH('cart_item', 'fulfillment_type') IS NULL ALTER TABLE cart_item ADD fulfillment_type VARCHAR(255) NULL",
                "UPDATE cart_item SET fulfillment_type = 'SHIPPING' WHERE fulfillment_type IS NULL",
                "IF OBJECT_ID('order_items', 'U') IS NOT NULL AND COL_LENGTH('order_items', 'product_name_snapshot') IS NULL ALTER TABLE order_items ADD product_name_snapshot VARCHAR(255) NULL",
                "IF OBJECT_ID('order_items', 'U') IS NOT NULL UPDATE oi SET product_name_snapshot = p.name FROM order_items oi JOIN products p ON oi.product_id = p.id WHERE oi.product_name_snapshot IS NULL",
                "IF COL_LENGTH('orders', 'parent_order_id') IS NOT NULL AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'orders' AND COLUMN_NAME = 'parent_order_id' AND DATA_TYPE = 'bigint') ALTER TABLE orders ALTER COLUMN parent_order_id INT NULL",
                "IF OBJECT_ID('order_items', 'U') IS NOT NULL AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'order_items' AND COLUMN_NAME = 'order_id' AND DATA_TYPE = 'bigint') ALTER TABLE order_items ALTER COLUMN order_id INT NOT NULL",
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
                "IF OBJECT_ID('fk_product_car_models_product', 'F') IS NULL AND OBJECT_ID('products', 'U') IS NOT NULL ALTER TABLE product_car_models ADD CONSTRAINT fk_product_car_models_product FOREIGN KEY (product_id) REFERENCES products(id)",
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
                "IF COL_LENGTH('vehicle', 'brand') IS NOT NULL UPDATE vehicle SET brand = 'Unknown' WHERE brand IS NULL",
                "IF COL_LENGTH('vehicle', 'model_name') IS NOT NULL UPDATE vehicle SET model_name = 'Unknown' WHERE model_name IS NULL",
                "IF COL_LENGTH('vehicle', 'year') IS NOT NULL UPDATE vehicle SET [year] = YEAR(GETDATE()) WHERE [year] IS NULL",
                "IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'vehicle' AND COLUMN_NAME = 'car_model_id' AND IS_NULLABLE = 'NO') ALTER TABLE vehicle ALTER COLUMN car_model_id INT NULL",
                "IF COL_LENGTH('vehicle', 'car_model_id') IS NOT NULL UPDATE vehicle SET car_model_id = NULL"
        );
        statements.forEach(statement -> {
            try {
                jdbcTemplate.execute(statement);
            } catch (RuntimeException ignored) {
                // Keep local startup tolerant if SQL Server blocks altering an older FK column.
            }
        });
    }
}
