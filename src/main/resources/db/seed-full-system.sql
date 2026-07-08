/* GearShift Pro full demo seed data for SQL Server.
   Run manually when you want to load a fresh demo catalog.

   Notes:
   - car_model is the system catalog used to filter compatible products.
   - vehicle is only a customer's personal vehicle used for booking demos.
   - This script is idempotent by natural keys such as email, SKU, service name,
     category name, car brand/model/year, and order code-like demo records.
*/

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

DECLARE @password_hash NVARCHAR(255) = N'$2a$10$O3OGQjdH.WH.LHpGBMF6Pux36EG1ewV54RpXH0QoLn91827QZo.Ai'; -- 123456

/* Users */
IF NOT EXISTS (SELECT 1 FROM users WHERE email = N'admin@gearshift.vn')
    INSERT INTO users (email, password_hash, full_name, phone, address, role, status)
    VALUES (N'admin@gearshift.vn', @password_hash, N'Admin GearShift', N'0900000001', N'FPT University', N'ADMIN', N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = N'staff@gearshift.vn')
    INSERT INTO users (email, password_hash, full_name, phone, address, role, status)
    VALUES (N'staff@gearshift.vn', @password_hash, N'Staff GearShift', N'0900000002', N'GearShift Workshop', N'STAFF', N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = N'customer@gearshift.vn')
    INSERT INTO users (email, password_hash, full_name, phone, address, role, status)
    VALUES (N'customer@gearshift.vn', @password_hash, N'Nguyen Van Customer', N'0900000003', N'Quan 9, TP Thu Duc', N'CUSTOMER', N'ACTIVE');

/* Product categories */
DECLARE @categories TABLE (category_name NVARCHAR(100), description NVARCHAR(MAX));
INSERT INTO @categories (category_name, description) VALUES
(N'Engine Parts', N'Phu tung dong co: turbo, loc gio, loc dau, bugi, bom nuoc.'),
(N'Brakes', N'He thong phanh: bo thang, dia phanh, heo phanh va phu kien.'),
(N'Tires & Wheels', N'Mam, lop va phu kien banh xe.'),
(N'Suspension', N'Phu tung gam va he thong treo.'),
(N'Oil & Fluids', N'Dau nhot, dau hop so, nuoc lam mat va dung dich bao duong.'),
(N'Interior & Accessories', N'Phu kien noi that va tien ich.'),
(N'Other', N'Nhom phu tung khac.');

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
(N'Digital Diagnostics', N'Quet loi dien tu, kiem tra suc khoe ECU va bao cao tinh trang xe.', 120.00, 180.00, 60, N'ACTIVE'),
(N'Brake & Chassis Inspection', N'Kiem tra phanh, gam, treo va cac chi tiet duoi xe.', 280.00, 420.00, 90, N'ACTIVE'),
(N'Engine Performance Tuning', N'Kiem tra ECU, hieu chinh va danh gia hieu nang dong co.', 450.00, 750.00, 120, N'ACTIVE'),
(N'Regular Maintenance', N'Bao duong dinh ky: dau, loc, dung dich va kiem tra tong quat.', 180.00, 350.00, 60, N'ACTIVE'),
(N'Air Conditioning Service', N'Kiem tra dieu hoa, ve sinh loc gio va nap gas khi can.', 160.00, 320.00, 75, N'ACTIVE'),
(N'Wheel Alignment', N'Can chinh thuoc lai, kiem tra lop va goc dat banh xe.', 220.00, 360.00, 75, N'ACTIVE');

INSERT INTO service (service_name, description, min_price, max_price, duration_minutes, status)
SELECT s.service_name, s.description, s.min_price, s.max_price, s.duration_minutes, s.status
FROM @services s
WHERE NOT EXISTS (
    SELECT 1 FROM service existing WHERE existing.service_name = s.service_name
);

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
    physical_stock INT,
    reserved_stock INT,
    image_url NVARCHAR(255),
    description NVARCHAR(MAX),
    status NVARCHAR(20)
);
INSERT INTO @products (category_name, sku, name, price, physical_stock, reserved_stock, image_url, description, status) VALUES
(N'Engine Parts', N'SKU001', N'Hybrid Series Turbocharger', 2850.00, 8, 0, N'/images/turbocharger.jpg', N'Direct bolt-on turbo upgrade for high horsepower builds.', N'ACTIVE'),
(N'Engine Parts', N'SKU002', N'Performance Air Filter', 145.00, 30, 0, N'/images/air-filter.jpg', N'High-flow air filter for improved intake response.', N'ACTIVE'),
(N'Engine Parts', N'SKU003', N'Iridium Spark Plug Set', 95.00, 40, 0, N'/images/spark-plug.jpg', N'Premium iridium spark plugs for stable ignition.', N'ACTIVE'),
(N'Brakes', N'SKU004', N'Stage 2 Performance Brake Kit', 1249.99, 12, 0, N'/images/brake-kit.jpg', N'Street and track brake kit with upgraded rotors and pads.', N'ACTIVE'),
(N'Brakes', N'SKU005', N'Ceramic Brake Pad Set', 210.00, 18, 0, N'/images/brake-pad.jpg', N'Low-dust ceramic pads for daily performance driving.', N'ACTIVE'),
(N'Tires & Wheels', N'SKU006', N'Forged Alloy Rims', 2450.00, 6, 0, N'/images/alloy-rims.jpg', N'Lightweight satin black forged wheel set.', N'ACTIVE'),
(N'Tires & Wheels', N'SKU007', N'Track-Ready Tire Set', 1280.00, 16, 0, N'/images/tires.jpg', N'High-grip tire set for spirited driving.', N'ACTIVE'),
(N'Suspension', N'SKU008', N'Track-Spec Coilover Kit', 1890.00, 10, 0, N'/images/coilover.jpg', N'Height-adjustable coilover kit for sharper handling.', N'ACTIVE'),
(N'Suspension', N'SKU009', N'Front Control Arm Kit', 420.00, 14, 0, N'/images/control-arm.jpg', N'Complete front control arm replacement kit.', N'ACTIVE'),
(N'Oil & Fluids', N'SKU010', N'0W-30 Full Synthetic Oil', 74.99, 50, 0, N'/images/oil.jpg', N'Premium full synthetic oil for modern engines.', N'ACTIVE'),
(N'Oil & Fluids', N'SKU011', N'AISIN Super Long Life Coolant 4L', 680.00, 20, 0, N'/images/coolant.jpg', N'Super long-life coolant for Japanese and European vehicles.', N'ACTIVE'),
(N'Oil & Fluids', N'SKU012', N'Automatic Transmission Fluid AFW-VI', 1580.00, 0, 0, N'/images/atf.jpg', N'Automatic transmission fluid. Currently unavailable.', N'INACTIVE'),
(N'Interior & Accessories', N'SKU013', N'Carbon Fiber Shift Knob', 125.00, 22, 0, N'/images/shift-knob.jpg', N'Carbon-look shift knob with weighted feel.', N'ACTIVE'),
(N'Other', N'SKU014', N'Universal Emergency Road Kit', 88.00, 15, 0, N'/images/road-kit.jpg', N'Emergency roadside kit for daily drivers.', N'ACTIVE');

INSERT INTO products (category_id, sku, name, price, physical_stock, reserved_stock, image_url, description, status, version, created_at, updated_at)
SELECT c.category_id, p.sku, p.name, p.price, p.physical_stock, p.reserved_stock, p.image_url, p.description, p.status, 0, SYSDATETIME(), SYSDATETIME()
FROM @products p
JOIN category c ON c.category_name = p.category_name
WHERE NOT EXISTS (
    SELECT 1 FROM products existing WHERE existing.sku = p.sku
);

/* Product compatibility with system car model catalog */
INSERT INTO product_car_models (product_id, car_model_id)
SELECT p.id, cm.car_model_id
FROM products p
CROSS JOIN car_model cm
WHERE p.sku IN (N'SKU001', N'SKU002', N'SKU003', N'SKU010')
  AND cm.brand = N'BMW'
  AND NOT EXISTS (
      SELECT 1 FROM product_car_models existing
      WHERE existing.product_id = p.id AND existing.car_model_id = cm.car_model_id
  );

INSERT INTO product_car_models (product_id, car_model_id)
SELECT p.id, cm.car_model_id
FROM products p
CROSS JOIN car_model cm
WHERE p.sku IN (N'SKU004', N'SKU005', N'SKU010', N'SKU011', N'SKU012')
  AND cm.brand IN (N'Toyota', N'Honda', N'Mazda')
  AND NOT EXISTS (
      SELECT 1 FROM product_car_models existing
      WHERE existing.product_id = p.id AND existing.car_model_id = cm.car_model_id
  );

INSERT INTO product_car_models (product_id, car_model_id)
SELECT p.id, cm.car_model_id
FROM products p
CROSS JOIN car_model cm
WHERE p.sku IN (N'SKU001', N'SKU006', N'SKU007', N'SKU008', N'SKU009', N'SKU010')
  AND cm.brand IN (N'Mercedes-Benz', N'Porsche')
  AND NOT EXISTS (
      SELECT 1 FROM product_car_models existing
      WHERE existing.product_id = p.id AND existing.car_model_id = cm.car_model_id
  );

/* Customer vehicle demo for booking only */
DECLARE @customer_id INT = (SELECT user_id FROM users WHERE email = N'customer@gearshift.vn');
DECLARE @staff_id INT = (SELECT user_id FROM users WHERE email = N'staff@gearshift.vn');
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
    JOIN products p ON p.id = ci.product_id
    WHERE ci.user_id = @customer_id AND p.sku = N'SKU010'
)
    INSERT INTO cart_item (user_id, product_id, quantity, fulfillment_type)
    SELECT @customer_id, p.id, 2, N'SHIPPING'
    FROM products p
    WHERE p.sku = N'SKU010';

/* Demo orders and order items */
IF @customer_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM orders
    WHERE user_id = @customer_id
      AND shipping_address = N'DEMO_SEED_ORDER_COMPLETED'
)
BEGIN
    INSERT INTO orders (user_id, order_type, order_status, payment_status, product_total, deposit_amount, remaining_amount, shipping_address, created_at, updated_at)
    VALUES (@customer_id, N'SHIPPING', N'COMPLETED', N'PAID', 2850.00, 0.00, 0.00, N'DEMO_SEED_ORDER_COMPLETED', DATEADD(DAY, -5, SYSDATETIME()), DATEADD(DAY, -5, SYSDATETIME()));

    DECLARE @completed_order_id INT = SCOPE_IDENTITY();

    INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total, product_name_snapshot, fulfillment_type, created_at)
    SELECT @completed_order_id, p.id, 1, p.price, p.price, p.name, N'SHIPPING', DATEADD(DAY, -5, SYSDATETIME())
    FROM products p
    WHERE p.sku = N'SKU001';
END;

IF @customer_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM orders
    WHERE user_id = @customer_id
      AND shipping_address = N'DEMO_SEED_ORDER_PENDING'
)
BEGIN
    INSERT INTO orders (user_id, order_type, order_status, payment_status, product_total, deposit_amount, remaining_amount, shipping_address, created_at, updated_at)
    VALUES (@customer_id, N'SHIPPING', N'PENDING_DEPOSIT', N'PENDING', 1280.00, 256.00, 1024.00, N'DEMO_SEED_ORDER_PENDING', DATEADD(DAY, -1, SYSDATETIME()), DATEADD(DAY, -1, SYSDATETIME()));

    DECLARE @pending_order_id INT = SCOPE_IDENTITY();

    INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total, product_name_snapshot, fulfillment_type, created_at)
    SELECT @pending_order_id, p.id, 1, p.price, p.price, p.name, N'SHIPPING', DATEADD(DAY, -1, SYSDATETIME())
    FROM products p
    WHERE p.sku = N'SKU007';

    INSERT INTO inventory_reservations (product_id, order_id, quantity, reservation_status, expires_at, created_at, updated_at)
    SELECT p.id, @pending_order_id, 1, N'HELD', DATEADD(HOUR, 24, SYSDATETIME()), SYSDATETIME(), SYSDATETIME()
    FROM products p
    WHERE p.sku = N'SKU007'
      AND NOT EXISTS (
          SELECT 1 FROM inventory_reservations existing
          WHERE existing.order_id = @pending_order_id AND existing.product_id = p.id
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
        deposit_amount, final_amount, remaining_amount, booking_status, payment_status,
        payment_deadline, notes, created_at, updated_at
    )
    VALUES (
        @customer_id, @vehicle_id, CONVERT(date, DATEADD(DAY, 2, SYSDATETIME())),
        CONVERT(time, '09:00'), CONVERT(time, '11:00'), N'09:00 - 11:00',
        120, 300.00, 530.00, 60.00, NULL, NULL, N'CONFIRMED', N'PAID',
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
    SELECT @booking_id, p.id, N'Thay dau dong co de xuat khi bao duong', 1, p.price, p.price,
           N'PENDING', @staff_id, SYSDATETIME(), SYSDATETIME()
    FROM products p
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
        @customer_id, @demo_pending_order_id, N'900000001', 256.00, N'PENDING',
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
    (SELECT COUNT(*) FROM products) AS total_products,
    (SELECT COUNT(*) FROM product_car_models) AS total_product_car_model_links,
    (SELECT COUNT(*) FROM bookings) AS total_bookings,
    (SELECT COUNT(*) FROM orders) AS total_orders;
