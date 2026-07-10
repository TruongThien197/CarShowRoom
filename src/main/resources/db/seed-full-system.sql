/* GearShift Pro full demo seed data for SQL Server.
   Run manually when you want to load a fresh demo catalog.

   Notes:
   - car_model is the system catalog used to filter compatible products.
   - vehicle is only a customer's personal vehicle used for booking demos.
   - This script resets demo data and then inserts a clean catalog.
   - Demo accounts keep the current emails and password 123456.
*/

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

DECLARE @password_hash NVARCHAR(255) = N'$2a$10$O3OGQjdH.WH.LHpGBMF6Pux36EG1ewV54RpXH0QoLn91827QZo.Ai'; -- 123456

/* Clean demo data in FK-safe order. Run this on local/demo databases only. */
IF OBJECT_ID(N'payment_transactions', N'U') IS NOT NULL DELETE FROM payment_transactions;
IF OBJECT_ID(N'inventory_reservations', N'U') IS NOT NULL DELETE FROM inventory_reservations;
IF OBJECT_ID(N'notifications', N'U') IS NOT NULL DELETE FROM notifications;
IF OBJECT_ID(N'booking_extra_items', N'U') IS NOT NULL DELETE FROM booking_extra_items;
IF OBJECT_ID(N'booking_services', N'U') IS NOT NULL DELETE FROM booking_services;
IF OBJECT_ID(N'booking_detail', N'U') IS NOT NULL DELETE FROM booking_detail;
IF OBJECT_ID(N'bookings', N'U') IS NOT NULL DELETE FROM bookings;
IF OBJECT_ID(N'booking', N'U') IS NOT NULL DELETE FROM booking;
IF OBJECT_ID(N'order_items', N'U') IS NOT NULL DELETE FROM order_items;
IF OBJECT_ID(N'order_detail', N'U') IS NOT NULL DELETE FROM order_detail;
IF OBJECT_ID(N'orders', N'U') IS NOT NULL DELETE FROM orders;
IF OBJECT_ID(N'cart_item', N'U') IS NOT NULL DELETE FROM cart_item;
IF OBJECT_ID(N'vehicle', N'U') IS NOT NULL DELETE FROM vehicle;
IF OBJECT_ID(N'product_car_models', N'U') IS NOT NULL DELETE FROM product_car_models;
IF OBJECT_ID(N'product_compatibility', N'U') IS NOT NULL DELETE FROM product_compatibility;
IF OBJECT_ID(N'product', N'U') IS NOT NULL DELETE FROM product;
IF OBJECT_ID(N'service', N'U') IS NOT NULL DELETE FROM service;
IF OBJECT_ID(N'car_model', N'U') IS NOT NULL DELETE FROM car_model;
IF OBJECT_ID(N'category', N'U') IS NOT NULL DELETE FROM category;
DELETE FROM users WHERE email IN (
    N'admin@gearshift.local', N'staff@gearshift.local', N'customer@gearshift.local',
    N'admin@gearshift.vn', N'staff@gearshift.vn', N'customer@gearshift.vn'
);

/* Users */
IF NOT EXISTS (SELECT 1 FROM users WHERE email = N'admin@gearshift.local')
    INSERT INTO users (email, password_hash, full_name, phone, address, role, status)
    VALUES (N'admin@gearshift.local', @password_hash, N'Admin GearShift', N'0900000001', N'Showroom Office', N'ADMIN', N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = N'staff@gearshift.local')
    INSERT INTO users (email, password_hash, full_name, phone, address, role, status)
    VALUES (N'staff@gearshift.local', @password_hash, N'Staff GearShift', N'0900000002', N'Service Bay', N'STAFF', N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = N'customer@gearshift.local')
    INSERT INTO users (email, password_hash, full_name, phone, address, role, status)
    VALUES (N'customer@gearshift.local', @password_hash, N'Customer Demo', N'0900000003', N'123 Demo Street', N'CUSTOMER', N'ACTIVE');

/* Product categories */
DECLARE @categories TABLE (category_name NVARCHAR(100), description NVARCHAR(MAX));
INSERT INTO @categories (category_name, description) VALUES
(N'Engine Parts', N'Engine parts: turbochargers, air filters, oil filters, spark plugs, water pumps.'),
(N'Brakes', N'Brake system: brake kits, brake rotors, brake calipers, and accessories.'),
(N'Tires & Wheels', N'Wheels, tires, and wheel accessories.'),
(N'Suspension', N'Chassis and suspension parts.'),
(N'Oil & Fluids', N'Engine oil, transmission fluid, coolant, and maintenance fluids.'),
(N'Interior & Accessories', N'Interior accessories and conveniences.'),
(N'Other', N'Other parts and accessories.');

INSERT INTO category (category_name, description)
SELECT c.category_name, c.description
FROM @categories c
WHERE NOT EXISTS (
    SELECT 1 FROM category existing WHERE existing.category_name = c.category_name
);

/* Booking service catalog */
DECLARE @services TABLE (
    service_name NVARCHAR(150),
    description NVARCHAR(MAX),
    min_price DECIMAL(18,2),
    max_price DECIMAL(18,2),
    duration_minutes INT,
    status NVARCHAR(20)
);
INSERT INTO @services (service_name, description, min_price, max_price, duration_minutes, status) VALUES
(N'Digital Diagnostics', N'Electronic fault scan, ECU health check, and vehicle condition report.', 20000, 30000, 60, N'ACTIVE'),
(N'Brake & Chassis Inspection', N'Inspection of brakes, undercarriage, suspension, and related components.', 35000, 45000, 90, N'ACTIVE'),
(N'Engine Performance Tuning', N'ECU check, calibration, and engine performance evaluation.', 40000, 60000, 120, N'ACTIVE'),
(N'Regular Maintenance', N'Routine maintenance: oil, filters, fluids, and general inspection.', 30000, 45000, 60, N'ACTIVE'),
(N'Air Conditioning Service', N'A/C system check, air filter cleaning, and refrigerant recharge as needed.', 20000, 30000, 75, N'ACTIVE'),
(N'Wheel Alignment', N'Steering alignment, tire inspection, and wheel angle adjustment.', 20000, 35000, 75, N'ACTIVE');

IF COL_LENGTH(N'service', N'price') IS NOT NULL
AND EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = N'service'
      AND COLUMN_NAME = N'price'
      AND IS_NULLABLE = N'NO'
)
    ALTER TABLE service ALTER COLUMN price DECIMAL(18,2) NULL;

INSERT INTO service (service_name, description, min_price, max_price, duration_minutes, status)
SELECT s.service_name, s.description, s.min_price, s.max_price, s.duration_minutes, s.status
FROM @services s
WHERE NOT EXISTS (
    SELECT 1 FROM service existing WHERE existing.service_name = s.service_name
);

UPDATE service SET min_price = 0 WHERE min_price IS NULL;
UPDATE service SET max_price = min_price WHERE max_price IS NULL;
UPDATE service SET duration_minutes = 60 WHERE duration_minutes IS NULL;
UPDATE service SET status = N'ACTIVE' WHERE status IS NULL;

IF NOT EXISTS (SELECT 1 FROM service WHERE min_price IS NULL)
AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = N'service' AND COLUMN_NAME = N'min_price' AND IS_NULLABLE = N'YES')
    ALTER TABLE service ALTER COLUMN min_price DECIMAL(18,2) NOT NULL;

IF NOT EXISTS (SELECT 1 FROM service WHERE max_price IS NULL)
AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = N'service' AND COLUMN_NAME = N'max_price' AND IS_NULLABLE = N'YES')
    ALTER TABLE service ALTER COLUMN max_price DECIMAL(18,2) NOT NULL;

IF NOT EXISTS (SELECT 1 FROM service WHERE duration_minutes IS NULL)
AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = N'service' AND COLUMN_NAME = N'duration_minutes' AND IS_NULLABLE = N'YES')
    ALTER TABLE service ALTER COLUMN duration_minutes INT NOT NULL;

IF NOT EXISTS (SELECT 1 FROM service WHERE status IS NULL)
AND EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = N'service' AND COLUMN_NAME = N'status' AND IS_NULLABLE = N'YES')
    ALTER TABLE service ALTER COLUMN status VARCHAR(255) NOT NULL;

/* System car model catalog */
DECLARE @car_models TABLE (brand NVARCHAR(100), model_name NVARCHAR(100), [year] INT);
INSERT INTO @car_models (brand, model_name, [year]) VALUES
(N'BMW', N'114i', 2023),
(N'BMW', N'116i', 2023),
(N'BMW', N'118i', 2023),
(N'BMW', N'120d', 2023),
(N'BMW', N'120i', 2023),
(N'BMW', N'128i', 2023),
(N'BMW', N'135is', 2023),
(N'BMW', N'218i', 2023),
(N'BMW', N'316d', 2023),
(N'BMW', N'316i', 2023),
(N'BMW', N'318ci', 2023),
(N'BMW', N'318d', 2023),
(N'BMW', N'318i', 2023),
(N'BMW', N'318i/320i', 2023),
(N'BMW', N'318is', 2023),
(N'BMW', N'320d', 2023),
(N'BMW', N'320i', 2023),
(N'BMW', N'325ci', 2023),
(N'BMW', N'325i', 2023),
(N'BMW', N'325i ul', 2023),
(N'BMW', N'328i', 2023),
(N'BMW', N'328xi', 2023),
(N'BMW', N'330ci', 2023),
(N'BMW', N'330i', 2023),
(N'BMW', N'330xi', 2023),
(N'BMW', N'335i', 2023),
(N'BMW', N'335is', 2023),
(N'BMW', N'335Xi', 2023),
(N'BMW', N'420i', 2023),
(N'BMW', N'428i', 2023),
(N'BMW', N'430i', 2023),
(N'BMW', N'440i', 2023),
(N'BMW', N'520d', 2023),
(N'BMW', N'520i', 2023),
(N'BMW', N'523i', 2023),
(N'BMW', N'525d', 2023),
(N'BMW', N'525i', 2023),
(N'BMW', N'525xi', 2023),
(N'BMW', N'528i', 2023),
(N'BMW', N'528xi', 2023),
(N'BMW', N'530d', 2023),
(N'BMW', N'530dx', 2023),
(N'BMW', N'530i', 2023),
(N'BMW', N'530xi', 2023),
(N'BMW', N'535d', 2023),
(N'BMW', N'535i', 2023),
(N'BMW', N'535ix', 2023),
(N'BMW', N'540i', 2023),
(N'BMW', N'545i', 2023),
(N'BMW', N'550i', 2023),
(N'BMW', N'M3', 2020),
(N'BMW', N'M4 G82', 2024),
(N'BMW', N'X5', 2022),
(N'Toyota', N'Camry', 2022),
(N'Toyota', N'Corolla Cross', 2023),
(N'Toyota', N'Fortuner', 2021),
(N'Toyota', N'Supra GR', 2022),
(N'Honda', N'Civic', 2022),
(N'Honda', N'Accord', 2021),
(N'Honda', N'CR-V', 2023),
(N'Mazda', N'Mazda 3', 2022),
(N'Mazda', N'Mazda 6', 2021),
(N'Mazda', N'CX-5', 2023),
(N'Mercedes-Benz', N'C200', 2022),
(N'Mercedes-Benz', N'E300', 2021),
(N'Mercedes-Benz', N'GLC 300', 2023),
(N'Porsche', N'911 Carrera', 2023),
(N'Porsche', N'Cayenne', 2022),
(N'Porsche', N'Macan', 2021);

INSERT INTO car_model (brand, model_name, [year])
SELECT cm.brand, cm.model_name, cm.[year]
FROM @car_models cm
WHERE NOT EXISTS (
    SELECT 1
    FROM car_model existing
    WHERE existing.brand = cm.brand
      AND existing.model_name = cm.model_name
      AND existing.[year] = cm.[year]
);

/* Products */
DECLARE @products TABLE (
    category_name NVARCHAR(100),
    sku NVARCHAR(255),
    name NVARCHAR(255),
    price DECIMAL(18,2),
    stock_quantity INT,
    reserved_stock INT,
    image_url NVARCHAR(255),
    description NVARCHAR(MAX),
    status NVARCHAR(20)
);
INSERT INTO @products (category_name, sku, name, price, stock_quantity, reserved_stock, image_url, description, status) VALUES
(N'Engine Parts', N'SKU001', N'Hybrid Series Turbocharger', 10000, 8, 0, N'/images/turbocharger.jpg', N'Direct bolt-on turbo upgrade for high horsepower builds.', N'ACTIVE'),
(N'Engine Parts', N'SKU002', N'Performance Air Filter', 12000, 30, 0, N'/images/turbocharger.jpg', N'High-flow air filter for improved intake response.', N'ACTIVE'),
(N'Engine Parts', N'SKU003', N'Iridium Spark Plug Set', 14000, 40, 0, N'/images/turbocharger.jpg', N'Premium iridium spark plugs for stable ignition.', N'ACTIVE'),
(N'Brakes', N'SKU004', N'Stage 2 Performance Brake Kit', 16000, 12, 0, N'/images/suspension-service.jpg', N'Street and track brake kit with upgraded rotors and pads.', N'ACTIVE'),
(N'Brakes', N'SKU005', N'Ceramic Brake Pad Set', 18000, 18, 0, N'/images/suspension-service.jpg', N'Low-dust ceramic pads for daily performance driving.', N'ACTIVE'),
(N'Tires & Wheels', N'SKU006', N'Forged Alloy Rims', 20000, 6, 0, N'/images/forged-rims.jpg', N'Lightweight satin black forged wheel set.', N'ACTIVE'),
(N'Tires & Wheels', N'SKU007', N'Track-Ready Tire Set', 22000, 16, 0, N'/images/track-tire.jpg', N'High-grip tire set for daily performance and weekend track use.', N'ACTIVE'),
(N'Suspension', N'SKU008', N'Track-Spec Coilover Kit', 24000, 10, 0, N'/images/suspension-service.jpg', N'Adjustable coilover kit for sharper handling and ride control.', N'ACTIVE'),
(N'Suspension', N'SKU009', N'Front Control Arm Kit', 26000, 14, 0, N'/images/suspension-service.jpg', N'Complete front control arm replacement kit.', N'ACTIVE'),
(N'Oil & Fluids', N'SKU010', N'0W-30 Full Synthetic Oil', 28000, 50, 0, N'/images/turbocharger.jpg', N'Premium synthetic oil for modern engines.', N'ACTIVE'),
(N'Oil & Fluids', N'SKU011', N'AISIN Super Long Life Coolant 4L', 30000, 20, 0, N'/images/turbocharger.jpg', N'Super long-life coolant for Japanese and European vehicles.', N'ACTIVE'),
(N'Oil & Fluids', N'SKU012', N'Automatic Transmission Fluid AFW-VI', 12000, 0, 0, N'/images/turbocharger.jpg', N'Automatic transmission fluid. Currently unavailable.', N'INACTIVE'),
(N'Interior & Accessories', N'SKU013', N'Carbon Fiber Shift Knob', 14000, 22, 0, N'/images/forged-rims.jpg', N'Carbon-look shift knob with weighted feel.', N'ACTIVE'),
(N'Other', N'SKU014', N'Universal Emergency Road Kit', 16000, 15, 0, N'/images/track-tire.jpg', N'Emergency roadside kit for daily drivers.', N'ACTIVE');

INSERT INTO product (category_id, sku, product_name, price, stock_quantity, reserved_stock, image_url, description, status, version, created_at, updated_at)
SELECT c.category_id, p.sku, p.name, p.price, p.stock_quantity, p.reserved_stock, p.image_url, p.description, p.status, 0, SYSDATETIME(), SYSDATETIME()
FROM @products p
JOIN category c ON c.category_name = p.category_name
WHERE NOT EXISTS (
    SELECT 1 FROM product existing WHERE existing.sku = p.sku
);

/* Product compatibility with system car model catalog */
INSERT INTO product_car_models (product_id, car_model_id)
SELECT p.product_id, cm.car_model_id
FROM product p
CROSS JOIN car_model cm
WHERE p.sku IN (N'SKU001', N'SKU002', N'SKU003', N'SKU010')
  AND cm.brand = N'BMW'
  AND NOT EXISTS (
      SELECT 1 FROM product_car_models existing
      WHERE existing.product_id = p.product_id AND existing.car_model_id = cm.car_model_id
  );

INSERT INTO product_car_models (product_id, car_model_id)
SELECT p.product_id, cm.car_model_id
FROM product p
CROSS JOIN car_model cm
WHERE p.sku IN (N'SKU004', N'SKU005', N'SKU010', N'SKU011', N'SKU012')
  AND cm.brand IN (N'Toyota', N'Honda', N'Mazda')
  AND NOT EXISTS (
      SELECT 1 FROM product_car_models existing
      WHERE existing.product_id = p.product_id AND existing.car_model_id = cm.car_model_id
  );

INSERT INTO product_car_models (product_id, car_model_id)
SELECT p.product_id, cm.car_model_id
FROM product p
CROSS JOIN car_model cm
WHERE p.sku IN (N'SKU001', N'SKU006', N'SKU007', N'SKU008', N'SKU009', N'SKU010')
  AND cm.brand IN (N'Mercedes-Benz', N'Porsche')
  AND NOT EXISTS (
      SELECT 1 FROM product_car_models existing
      WHERE existing.product_id = p.product_id AND existing.car_model_id = cm.car_model_id
  );

/* Customer vehicle demo for booking only */
DECLARE @customer_id INT = (SELECT user_id FROM users WHERE email = N'customer@gearshift.local');
DECLARE @staff_id INT = (SELECT user_id FROM users WHERE email = N'staff@gearshift.local');
DECLARE @honda_civic_id INT = (
    SELECT TOP 1 car_model_id FROM car_model
    WHERE brand = N'Honda' AND model_name = N'Civic' AND [year] = 2022
);

IF @customer_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM vehicle WHERE user_id = @customer_id AND license_plate = N'51G-30206')
    INSERT INTO vehicle (user_id, car_model_id, brand, model_name, [year], license_plate)
    VALUES (@customer_id, @honda_civic_id, N'Honda', N'Civic', 2022, N'51G-30206');

/* Demo cart */
IF @customer_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM cart_item ci
    JOIN product p ON p.product_id = ci.product_id
    WHERE ci.user_id = @customer_id AND p.sku = N'SKU010'
)
    INSERT INTO cart_item (user_id, product_id, quantity, fulfillment_type)
    SELECT @customer_id, p.product_id, 2, N'SHIPPING'
    FROM product p
    WHERE p.sku = N'SKU010';

/* Demo orders and order items */
IF @customer_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM orders
    WHERE user_id = @customer_id
      AND shipping_address = N'DEMO_SEED_ORDER_COMPLETED'
)
BEGIN
    INSERT INTO orders (user_id, order_type, order_status, payment_status, product_total, shipping_address, created_at, updated_at)
    VALUES (@customer_id, N'SHIPPING', N'COMPLETED', N'PAID', 10000, N'DEMO_SEED_ORDER_COMPLETED', DATEADD(DAY, -5, SYSDATETIME()), DATEADD(DAY, -5, SYSDATETIME()));

    DECLARE @completed_order_id INT = SCOPE_IDENTITY();

    INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total, product_name_snapshot, fulfillment_type, created_at)
    SELECT @completed_order_id, p.product_id, 1, p.price, p.price, p.product_name, N'SHIPPING', DATEADD(DAY, -5, SYSDATETIME())
    FROM product p
    WHERE p.sku = N'SKU001';
END;

IF @customer_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM orders
    WHERE user_id = @customer_id
      AND shipping_address = N'DEMO_SEED_ORDER_PENDING'
)
BEGIN
    INSERT INTO orders (user_id, order_type, order_status, payment_status, product_total, shipping_address, created_at, updated_at)
    VALUES (@customer_id, N'SHIPPING', N'PENDING_PAYMENT', N'PENDING', 22000, N'DEMO_SEED_ORDER_PENDING', DATEADD(DAY, -1, SYSDATETIME()), DATEADD(DAY, -1, SYSDATETIME()));

    DECLARE @pending_order_id INT = SCOPE_IDENTITY();

    INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total, product_name_snapshot, fulfillment_type, created_at)
    SELECT @pending_order_id, p.product_id, 1, p.price, p.price, p.product_name, N'SHIPPING', DATEADD(DAY, -1, SYSDATETIME())
    FROM product p
    WHERE p.sku = N'SKU007';

    INSERT INTO inventory_reservations (product_id, order_id, quantity, reservation_status, expires_at, created_at, updated_at)
    SELECT p.product_id, @pending_order_id, 1, N'HELD', DATEADD(HOUR, 24, SYSDATETIME()), SYSDATETIME(), SYSDATETIME()
    FROM product p
    WHERE p.sku = N'SKU007'
      AND NOT EXISTS (
          SELECT 1 FROM inventory_reservations existing
          WHERE existing.order_id = @pending_order_id AND existing.product_id = p.product_id
      );
END;

/* Demo booking */
DECLARE @vehicle_id INT = (
    SELECT TOP 1 vehicle_id FROM vehicle WHERE user_id = @customer_id AND license_plate = N'51G-30206'
);
DECLARE @diagnostics_id INT = (SELECT service_id FROM service WHERE service_name = N'Digital Diagnostics');
DECLARE @maintenance_id INT = (SELECT service_id FROM service WHERE service_name = N'Regular Maintenance');

IF @customer_id IS NOT NULL
AND @vehicle_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM bookings
    WHERE user_id = @customer_id
      AND vehicle_id = @vehicle_id
      AND notes = N'DEMO_SEED_BOOKING_CONFIRMED'
)
BEGIN
    INSERT INTO bookings (
        user_id, vehicle_id, booking_date, start_time, end_time, time_slot,
        total_duration_minutes, estimated_min_amount, estimated_max_amount,
        final_amount, booking_status, payment_status,
        payment_deadline, notes, created_at, updated_at
    )
    VALUES (
        @customer_id, @vehicle_id, CONVERT(date, DATEADD(DAY, 2, SYSDATETIME())),
        CONVERT(time, '09:00'), CONVERT(time, '11:00'), N'09:00 - 11:00',
        120, 30000, 75000, 30000, N'CONFIRMED', N'PAID',
        DATEADD(HOUR, 24, SYSDATETIME()), N'DEMO_SEED_BOOKING_CONFIRMED',
        SYSDATETIME(), SYSDATETIME()
    );

    DECLARE @booking_id INT = SCOPE_IDENTITY();

    INSERT INTO booking_services (booking_id, service_id, service_name_snapshot, duration_minutes_snapshot, min_price_snapshot, max_price_snapshot, created_at)
    SELECT @booking_id, s.service_id, s.service_name, s.duration_minutes, s.min_price, s.max_price, SYSDATETIME()
    FROM service s
    WHERE s.service_id IN (@diagnostics_id, @maintenance_id);

    INSERT INTO booking_extra_items (
        booking_id, product_id, description, quantity, unit_price, line_total,
        approval_status, created_by_staff_id, created_at, updated_at
    )
    SELECT @booking_id, p.product_id, N'Recommended engine oil change during maintenance', 1, p.price, p.price,
           N'PENDING', @staff_id, SYSDATETIME(), SYSDATETIME()
    FROM product p
    WHERE p.sku = N'SKU010';
END;

/* Demo payment rows for PayOS workflow screens. Real API keys/config are not seeded here. */
IF @customer_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM payment_transactions WHERE payos_order_code = N'900000001')
BEGIN
    DECLARE @demo_pending_order_id INT = (
        SELECT TOP 1 order_id
        FROM orders
        WHERE user_id = @customer_id AND shipping_address = N'DEMO_SEED_ORDER_PENDING'
        ORDER BY order_id DESC
    );

    INSERT INTO payment_transactions (
        user_id, order_id, payos_order_code, amount, status, checkout_url,
        payment_deadline, paid_at, raw_webhook_payload, created_at, updated_at
    )
    VALUES (
        @customer_id, @demo_pending_order_id, N'900000001', 22000, N'PENDING',
        N'https://pay.payos.vn/demo/900000001', DATEADD(HOUR, 24, SYSDATETIME()),
        NULL, NULL, SYSDATETIME(), SYSDATETIME()
    );
END;

COMMIT TRANSACTION;

SELECT
    (SELECT COUNT(*) FROM users) AS total_users,
    (SELECT COUNT(*) FROM category) AS total_categories,
    (SELECT COUNT(*) FROM service) AS total_services,
    (SELECT COUNT(*) FROM car_model) AS total_car_models,
    (SELECT COUNT(*) FROM product) AS total_products,
    (SELECT COUNT(*) FROM product_car_models) AS total_product_car_model_links,
    (SELECT COUNT(*) FROM bookings) AS total_bookings,
    (SELECT COUNT(*) FROM orders) AS total_orders;
