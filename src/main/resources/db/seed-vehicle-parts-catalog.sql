/*
   Product catalog for 70 vehicle models.
   Each model receives 5 categories with 2 products each.
   Every product is linked to exactly one car model and has no image.
*/
USE CarShowRoom;
GO

SET NOCOUNT ON;

-- Keep the product referenced by the existing order, then replace all other demo products.
DECLARE @preserved_product_id INT = (
    SELECT MIN(product_id) FROM order_items
);

IF (SELECT COUNT(DISTINCT product_id) FROM order_items) > 1
    THROW 50002, N'Không thể làm mới danh mục khi lịch sử đơn hàng tham chiếu nhiều sản phẩm.', 1;

IF @preserved_product_id IS NULL
    SELECT @preserved_product_id = MIN(product_id) FROM product;

IF @preserved_product_id IS NULL
    THROW 50001, N'Không tìm thấy sản phẩm để khởi tạo danh mục.', 1;

-- The five classifications used for every vehicle model.
IF NOT EXISTS (SELECT 1 FROM category WHERE category_name = N'Động cơ')
    INSERT INTO category (category_name, description) VALUES (N'Động cơ', N'Phụ tùng bảo dưỡng và vận hành động cơ.');
IF NOT EXISTS (SELECT 1 FROM category WHERE category_name = N'Hệ thống phanh')
    INSERT INTO category (category_name, description) VALUES (N'Hệ thống phanh', N'Phụ tùng thuộc hệ thống phanh của xe.');
IF NOT EXISTS (SELECT 1 FROM category WHERE category_name = N'Hệ thống treo')
    INSERT INTO category (category_name, description) VALUES (N'Hệ thống treo', N'Phụ tùng thuộc hệ thống treo và gầm xe.');
IF NOT EXISTS (SELECT 1 FROM category WHERE category_name = N'Lọc và dầu nhớt')
    INSERT INTO category (category_name, description) VALUES (N'Lọc và dầu nhớt', N'Bộ lọc và dung dịch bảo dưỡng định kỳ.');
IF NOT EXISTS (SELECT 1 FROM category WHERE category_name = N'Điện và chiếu sáng')
    INSERT INTO category (category_name, description) VALUES (N'Điện và chiếu sáng', N'Phụ tùng điện và chiếu sáng cho xe.');

DELETE FROM product_car_models;
DELETE FROM product WHERE product_id <> @preserved_product_id;

DECLARE @engine_category_id INT = (SELECT category_id FROM category WHERE category_name = N'Động cơ');
UPDATE product
SET category_id = @engine_category_id,
    product_name = N'Bugi đánh lửa',
    sku = N'CAT-TEMP-ENG-01',
    description = N'Bugi đánh lửa thay thế, phù hợp với dòng xe được chỉ định.',
    price = 1500,
    stock_quantity = 20,
    reserved_stock = 0,
    image_url = NULL,
    status = N'ACTIVE',
    version = 0,
    updated_at = SYSDATETIME()
WHERE product_id = @preserved_product_id;

DELETE FROM category
WHERE category_name NOT IN (N'Động cơ', N'Hệ thống phanh', N'Hệ thống treo', N'Lọc và dầu nhớt', N'Điện và chiếu sáng');

DECLARE @catalog TABLE (
    ordinal INT IDENTITY(1,1) PRIMARY KEY,
    car_model_id INT NOT NULL,
    category_id INT NOT NULL,
    sku VARCHAR(255) NOT NULL UNIQUE,
    product_name NVARCHAR(150) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    stock_quantity INT NOT NULL
);

INSERT INTO @catalog (car_model_id, category_id, sku, product_name, description, price, stock_quantity)
SELECT cm.car_model_id,
       c.category_id,
       CONCAT(N'CAT-', cm.car_model_id, N'-', item.code, N'-', item.item_no),
       CONCAT(item.item_name, N' cho ', cm.brand, N' ', cm.model_name, N' ', cm.[year]),
       CONCAT(item.item_name, N' thay thế, tương thích riêng với ', cm.brand, N' ', cm.model_name, N' ', cm.[year], N'.'),
       item.price,
       item.stock_quantity
FROM car_model cm
CROSS JOIN (
    SELECT category_id, category_name FROM category
    WHERE category_name IN (N'Động cơ', N'Hệ thống phanh', N'Hệ thống treo', N'Lọc và dầu nhớt', N'Điện và chiếu sáng')
) c
CROSS APPLY (
    SELECT * FROM (VALUES
        (N'Động cơ', N'ENG', 1, N'Bugi đánh lửa', 1500, 20),
        (N'Động cơ', N'ENG', 2, N'Lọc gió động cơ', 1200, 25),
        (N'Hệ thống phanh', N'BRK', 1, N'Má phanh trước', 2500, 18),
        (N'Hệ thống phanh', N'BRK', 2, N'Đĩa phanh trước', 4500, 12),
        (N'Hệ thống treo', N'SUS', 1, N'Cao su càng A', 1800, 20),
        (N'Hệ thống treo', N'SUS', 2, N'Rotuyn cân bằng', 2200, 16),
        (N'Lọc và dầu nhớt', N'OIL', 1, N'Lọc dầu động cơ', 900, 30),
        (N'Lọc và dầu nhớt', N'OIL', 2, N'Dầu nhớt tổng hợp 5W-30', 3500, 24),
        (N'Điện và chiếu sáng', N'ELC', 1, N'Bóng đèn pha', 1100, 22),
        (N'Điện và chiếu sáng', N'ELC', 2, N'Cảm biến oxy', 3200, 14)
    ) AS v(category_name, code, item_no, item_name, price, stock_quantity)
    WHERE v.category_name = c.category_name
) item;

DECLARE @first_catalog_id INT = (SELECT MIN(ordinal) FROM @catalog);
DECLARE @first_sku VARCHAR(255) = (SELECT sku FROM @catalog WHERE ordinal = @first_catalog_id);
DECLARE @first_name NVARCHAR(150) = (SELECT product_name FROM @catalog WHERE ordinal = @first_catalog_id);
DECLARE @first_description NVARCHAR(MAX) = (SELECT description FROM @catalog WHERE ordinal = @first_catalog_id);
DECLARE @first_price DECIMAL(18,2) = (SELECT price FROM @catalog WHERE ordinal = @first_catalog_id);
DECLARE @first_stock INT = (SELECT stock_quantity FROM @catalog WHERE ordinal = @first_catalog_id);
DECLARE @first_category_id INT = (SELECT category_id FROM @catalog WHERE ordinal = @first_catalog_id);
DECLARE @first_car_model_id INT = (SELECT car_model_id FROM @catalog WHERE ordinal = @first_catalog_id);

UPDATE product
SET category_id = @first_category_id,
    product_name = @first_name,
    sku = @first_sku,
    description = @first_description,
    price = @first_price,
    stock_quantity = @first_stock,
    reserved_stock = 0,
    image_url = NULL,
    status = N'ACTIVE',
    version = 0,
    updated_at = SYSDATETIME()
WHERE product_id = @preserved_product_id;

INSERT INTO product (category_id, product_name, sku, description, price, stock_quantity, reserved_stock, image_url, status, version, created_at, updated_at)
SELECT category_id, product_name, sku, description, price, stock_quantity, 0, NULL, N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME()
FROM @catalog
WHERE ordinal <> @first_catalog_id;

INSERT INTO product_car_models (product_id, car_model_id)
SELECT p.product_id, c.car_model_id
FROM product p
JOIN @catalog c ON c.sku = p.sku;

SELECT
    (SELECT COUNT(*) FROM product) AS total_products,
    (SELECT COUNT(*) FROM product_car_models) AS total_product_car_model_links,
    (SELECT COUNT(*) FROM product WHERE image_url IS NULL) AS products_without_images,
    (SELECT MAX(price) FROM product) AS maximum_price_vnd;
GO
