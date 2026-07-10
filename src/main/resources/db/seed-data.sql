IF DB_ID(N'CarShowRoom') IS NULL
BEGIN
    CREATE DATABASE CarShowRoom;
END
GO

USE CarShowRoom;
GO

IF OBJECT_ID(N'users', N'U') IS NULL
BEGIN
    CREATE TABLE users (
        user_id INT IDENTITY(1,1) PRIMARY KEY,
        email NVARCHAR(255) NOT NULL UNIQUE,
        password_hash NVARCHAR(255) NOT NULL,
        jwt_refresh_token NVARCHAR(MAX) NULL,
        full_name NVARCHAR(150) NOT NULL,
        phone NVARCHAR(20) NULL,
        address NVARCHAR(255) NULL,
        role NVARCHAR(50) NOT NULL,
        status NVARCHAR(50) NOT NULL DEFAULT N'ACTIVE'
    );
END
GO

IF OBJECT_ID(N'category', N'U') IS NULL
BEGIN
    CREATE TABLE category (
        category_id INT IDENTITY(1,1) PRIMARY KEY,
        category_name NVARCHAR(100) NOT NULL UNIQUE,
        description NVARCHAR(MAX) NULL
    );
END
GO

IF OBJECT_ID(N'car_model', N'U') IS NULL
BEGIN
    CREATE TABLE car_model (
        car_model_id INT IDENTITY(1,1) PRIMARY KEY,
        brand NVARCHAR(100) NOT NULL,
        model_name NVARCHAR(100) NOT NULL,
        [year] INT NOT NULL
    );
END
GO

IF OBJECT_ID(N'product', N'U') IS NULL
BEGIN
    CREATE TABLE product (
        product_id INT IDENTITY(1,1) PRIMARY KEY,
        category_id INT NULL,
        product_name NVARCHAR(150) NOT NULL,
        sku VARCHAR(255) NULL UNIQUE,
        description NVARCHAR(MAX) NULL,
        price DECIMAL(18,2) NOT NULL,
        stock_quantity INT NOT NULL DEFAULT 0,
        reserved_stock INT NOT NULL DEFAULT 0,
        image_url NVARCHAR(500) NULL,
        status NVARCHAR(50) NOT NULL DEFAULT N'ACTIVE',
        version BIGINT NULL DEFAULT 0,
        created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_product_category FOREIGN KEY (category_id) REFERENCES category(category_id)
    );
END
GO

IF OBJECT_ID(N'service', N'U') IS NULL
BEGIN
    CREATE TABLE service (
        service_id INT IDENTITY(1,1) PRIMARY KEY,
        service_name NVARCHAR(150) NOT NULL UNIQUE,
        description NVARCHAR(MAX) NULL,
        min_price DECIMAL(18,2) NOT NULL,
        max_price DECIMAL(18,2) NOT NULL,
        duration_minutes INT NOT NULL,
        status NVARCHAR(50) NOT NULL DEFAULT N'ACTIVE'
    );
END
GO

IF OBJECT_ID(N'vehicle', N'U') IS NULL
BEGIN
    CREATE TABLE vehicle (
        vehicle_id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        car_model_id INT NULL,
        brand NVARCHAR(100) NOT NULL,
        model_name NVARCHAR(100) NOT NULL,
        [year] INT NOT NULL,
        license_plate NVARCHAR(30) NOT NULL,
        CONSTRAINT FK_vehicle_user FOREIGN KEY (user_id) REFERENCES users(user_id),
        CONSTRAINT FK_vehicle_car_model FOREIGN KEY (car_model_id) REFERENCES car_model(car_model_id)
    );
END
GO

IF OBJECT_ID(N'cart_item', N'U') IS NULL
BEGIN
    CREATE TABLE cart_item (
        cart_item_id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        product_id INT NOT NULL,
        quantity INT NOT NULL DEFAULT 1,
        fulfillment_type NVARCHAR(50) NOT NULL DEFAULT N'SHIPPING',
        CONSTRAINT FK_cart_item_user FOREIGN KEY (user_id) REFERENCES users(user_id),
        CONSTRAINT FK_cart_item_product FOREIGN KEY (product_id) REFERENCES product(product_id)
    );
END
GO

IF OBJECT_ID(N'orders', N'U') IS NULL
BEGIN
    CREATE TABLE orders (
        order_id INT IDENTITY(1,1) PRIMARY KEY,
        parent_order_id INT NULL,
        user_id INT NOT NULL,
        order_type NVARCHAR(50) NOT NULL,
        order_status NVARCHAR(50) NOT NULL,
        payment_status NVARCHAR(50) NOT NULL,
        product_total DECIMAL(18,2) NOT NULL DEFAULT 0,
        shipping_address NVARCHAR(255) NULL,
        created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_orders_parent FOREIGN KEY (parent_order_id) REFERENCES orders(order_id),
        CONSTRAINT FK_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id)
    );
END
GO

IF OBJECT_ID(N'order_items', N'U') IS NULL
BEGIN
    CREATE TABLE order_items (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        order_id INT NOT NULL,
        product_id INT NOT NULL,
        quantity INT NOT NULL,
        unit_price DECIMAL(18,2) NOT NULL,
        line_total DECIMAL(18,2) NOT NULL,
        product_name_snapshot NVARCHAR(150) NOT NULL,
        fulfillment_type NVARCHAR(50) NOT NULL,
        created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_order_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
        CONSTRAINT FK_order_items_product FOREIGN KEY (product_id) REFERENCES product(product_id)
    );
END
GO

IF OBJECT_ID(N'bookings', N'U') IS NULL
BEGIN
    CREATE TABLE bookings (
        id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        vehicle_id INT NULL,
        related_order_id INT NULL,
        booking_date DATE NOT NULL,
        start_time TIME NOT NULL,
        end_time TIME NOT NULL,
        time_slot NVARCHAR(50) NULL,
        total_duration_minutes INT NOT NULL,
        estimated_min_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
        estimated_max_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
        final_amount DECIMAL(18,2) NULL,
        booking_status NVARCHAR(50) NOT NULL,
        payment_status NVARCHAR(50) NOT NULL,
        payment_deadline DATETIME2 NULL,
        notes NVARCHAR(MAX) NULL,
        created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_bookings_user FOREIGN KEY (user_id) REFERENCES users(user_id),
        CONSTRAINT FK_bookings_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id),
        CONSTRAINT FK_bookings_related_order FOREIGN KEY (related_order_id) REFERENCES orders(order_id)
    );
END
GO

IF OBJECT_ID(N'booking_services', N'U') IS NULL
BEGIN
    CREATE TABLE booking_services (
        id INT IDENTITY(1,1) PRIMARY KEY,
        booking_id INT NOT NULL,
        service_id INT NOT NULL,
        service_name_snapshot NVARCHAR(150) NOT NULL,
        duration_minutes_snapshot INT NOT NULL,
        min_price_snapshot DECIMAL(18,2) NOT NULL,
        max_price_snapshot DECIMAL(18,2) NOT NULL,
        created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_booking_services_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
        CONSTRAINT FK_booking_services_service FOREIGN KEY (service_id) REFERENCES service(service_id)
    );
END
GO

IF OBJECT_ID(N'booking_detail', N'U') IS NULL
BEGIN
    CREATE TABLE booking_detail (
        booking_detail_id INT IDENTITY(1,1) PRIMARY KEY,
        booking_id INT NOT NULL,
        service_id INT NOT NULL,
        actual_price DECIMAL(18,2) NOT NULL,
        CONSTRAINT FK_booking_detail_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
        CONSTRAINT FK_booking_detail_service FOREIGN KEY (service_id) REFERENCES service(service_id)
    );
END
GO

IF OBJECT_ID(N'booking_extra_items', N'U') IS NULL
BEGIN
    CREATE TABLE booking_extra_items (
        id INT IDENTITY(1,1) PRIMARY KEY,
        booking_id INT NOT NULL,
        product_id INT NULL,
        service_id INT NULL,
        description NVARCHAR(255) NOT NULL,
        quantity INT NOT NULL DEFAULT 1,
        unit_price DECIMAL(18,2) NOT NULL,
        line_total DECIMAL(18,2) NOT NULL,
        approval_status NVARCHAR(50) NOT NULL,
        created_by_staff_id INT NULL,
        created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_booking_extra_items_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
        CONSTRAINT FK_booking_extra_items_product FOREIGN KEY (product_id) REFERENCES product(product_id),
        CONSTRAINT FK_booking_extra_items_service FOREIGN KEY (service_id) REFERENCES service(service_id),
        CONSTRAINT FK_booking_extra_items_staff FOREIGN KEY (created_by_staff_id) REFERENCES users(user_id)
    );
END
GO

IF OBJECT_ID(N'product_car_models', N'U') IS NULL
BEGIN
    CREATE TABLE product_car_models (
        product_id INT NOT NULL,
        car_model_id INT NOT NULL,
        CONSTRAINT PK_product_car_models PRIMARY KEY (product_id, car_model_id),
        CONSTRAINT FK_product_car_models_product FOREIGN KEY (product_id) REFERENCES product(product_id),
        CONSTRAINT FK_product_car_models_car_model FOREIGN KEY (car_model_id) REFERENCES car_model(car_model_id)
    );
END
GO

IF OBJECT_ID(N'product_compatibility', N'U') IS NULL
BEGIN
    CREATE TABLE product_compatibility (
        product_id INT NOT NULL,
        car_model_id INT NOT NULL,
        CONSTRAINT PK_product_compatibility PRIMARY KEY (product_id, car_model_id),
        CONSTRAINT FK_product_compatibility_product FOREIGN KEY (product_id) REFERENCES product(product_id),
        CONSTRAINT FK_product_compatibility_car_model FOREIGN KEY (car_model_id) REFERENCES car_model(car_model_id)
    );
END
GO

IF OBJECT_ID(N'order_detail', N'U') IS NULL
BEGIN
    CREATE TABLE order_detail (
        order_detail_id INT IDENTITY(1,1) PRIMARY KEY,
        order_id INT NOT NULL,
        product_id INT NOT NULL,
        quantity INT NOT NULL,
        unit_price DECIMAL(18,2) NOT NULL,
        CONSTRAINT FK_order_detail_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
        CONSTRAINT FK_order_detail_product FOREIGN KEY (product_id) REFERENCES product(product_id)
    );
END
GO

IF OBJECT_ID(N'inventory_reservations', N'U') IS NULL
BEGIN
    CREATE TABLE inventory_reservations (
        id INT IDENTITY(1,1) PRIMARY KEY,
        product_id INT NOT NULL,
        order_id INT NULL,
        booking_id INT NULL,
        quantity INT NOT NULL,
        reservation_status NVARCHAR(50) NOT NULL,
        expires_at DATETIME2 NULL,
        created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_inventory_reservations_product FOREIGN KEY (product_id) REFERENCES product(product_id),
        CONSTRAINT FK_inventory_reservations_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
        CONSTRAINT FK_inventory_reservations_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
    );
END
GO

IF OBJECT_ID(N'payment_transactions', N'U') IS NULL
BEGIN
    CREATE TABLE payment_transactions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NULL,
        parent_order_id INT NULL,
        order_id INT NULL,
        booking_id INT NULL,
        payos_order_code NVARCHAR(255) NOT NULL UNIQUE,
        amount DECIMAL(18,2) NOT NULL,
        status NVARCHAR(50) NOT NULL,
        checkout_url NVARCHAR(1000) NULL,
        payment_deadline DATETIME2 NOT NULL,
        paid_at DATETIME2 NULL,
        raw_webhook_payload NVARCHAR(MAX) NULL,
        created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_payment_transactions_user FOREIGN KEY (user_id) REFERENCES users(user_id),
        CONSTRAINT FK_payment_transactions_parent_order FOREIGN KEY (parent_order_id) REFERENCES orders(order_id),
        CONSTRAINT FK_payment_transactions_order FOREIGN KEY (order_id) REFERENCES orders(order_id),
        CONSTRAINT FK_payment_transactions_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
    );
END
GO

IF OBJECT_ID(N'notifications', N'U') IS NULL
BEGIN
    CREATE TABLE notifications (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        title NVARCHAR(255) NOT NULL,
        message NVARCHAR(1000) NOT NULL,
        notification_type NVARCHAR(100) NOT NULL,
        is_read BIT NOT NULL DEFAULT 0,
        created_at DATETIME2 NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_notifications_user FOREIGN KEY (user_id) REFERENCES users(user_id)
    );
END
GO

DECLARE @password_hash NVARCHAR(255) = N'$2a$10$O3OGQjdH.WH.LHpGBMF6Pux36EG1ewV54RpXH0QoLn91827QZo.Ai'; -- 123456

IF NOT EXISTS (SELECT 1 FROM users WHERE email = N'admin@gearshift.local')
    INSERT INTO users (email, password_hash, full_name, phone, address, role, status)
    VALUES (N'admin@gearshift.local', @password_hash, N'Admin GearShift', N'0900000001', N'Showroom Office', N'ADMIN', N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = N'staff@gearshift.local')
    INSERT INTO users (email, password_hash, full_name, phone, address, role, status)
    VALUES (N'staff@gearshift.local', @password_hash, N'Staff GearShift', N'0900000002', N'Service Bay', N'STAFF', N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = N'customer@gearshift.local')
    INSERT INTO users (email, password_hash, full_name, phone, address, role, status)
    VALUES (N'customer@gearshift.local', @password_hash, N'Customer Demo', N'0900000003', N'123 Demo Street', N'CUSTOMER', N'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM category WHERE category_name = N'Engine Parts')
    INSERT INTO category (category_name, description) VALUES
    (N'Engine Parts', N'Turbo, intake, exhaust, and engine performance parts.'),
    (N'Brakes', N'Brake pads, rotors, calipers, and brake upgrade kits.'),
    (N'Tires & Wheels', N'Performance tires, forged wheels, and fitment parts.'),
    (N'Suspension', N'Coilovers, springs, arms, and handling upgrades.'),
    (N'Oil & Fluids', N'Engine oil, coolant, brake fluid, and service fluids.');

IF NOT EXISTS (SELECT 1 FROM car_model WHERE brand = N'BMW' AND model_name = N'M4 G82' AND [year] = 2024)
    INSERT INTO car_model (brand, model_name, [year]) VALUES
    (N'BMW', N'M4 G82', 2024),
    (N'Porsche', N'911 Carrera', 2023),
    (N'Toyota', N'Supra GR', 2022),
    (N'Honda', N'Civic', 2022);

IF NOT EXISTS (SELECT 1 FROM product WHERE sku = N'SKU001')
BEGIN
    INSERT INTO product (category_id, product_name, sku, description, price, stock_quantity, reserved_stock, image_url, status, version, created_at, updated_at)
    VALUES
    ((SELECT category_id FROM category WHERE category_name = N'Engine Parts'), N'Hybrid Series Turbocharger', N'SKU001', N'Direct bolt-on turbo upgrade for high horsepower builds.', 10000, 8, 0, N'/images/turbocharger.jpg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME()),
    ((SELECT category_id FROM category WHERE category_name = N'Brakes'), N'Stage 2 Performance Brake Kit', N'SKU002', N'Street and track brake kit with upgraded rotors and pads.', 12000, 12, 0, N'/images/suspension-service.jpg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME()),
    ((SELECT category_id FROM category WHERE category_name = N'Tires & Wheels'), N'Forged Alloy Rims', N'SKU003', N'Lightweight satin black forged wheel set.', 14000, 6, 0, N'/images/forged-rims.jpg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME()),
    ((SELECT category_id FROM category WHERE category_name = N'Tires & Wheels'), N'Track-Ready Tire Set', N'SKU004', N'High-grip tire set for daily performance and weekend track use.', 16000, 16, 0, N'/images/track-tire.jpg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME()),
    ((SELECT category_id FROM category WHERE category_name = N'Suspension'), N'Track-Spec Coilover Kit', N'SKU005', N'Adjustable coilover kit for sharper handling and ride control.', 18000, 10, 0, N'/images/suspension-service.jpg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME()),
    ((SELECT category_id FROM category WHERE category_name = N'Oil & Fluids'), N'0W-30 Full Synthetic Oil', N'SKU006', N'Premium synthetic oil for modern engines.', 20000, 50, 0, N'/images/turbocharger.jpg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME());
END

IF NOT EXISTS (SELECT 1 FROM service WHERE service_name = N'Digital Diagnostics')
    INSERT INTO service (service_name, description, min_price, max_price, duration_minutes, status) VALUES
    (N'Digital Diagnostics', N'Full vehicle scan and system health report.', 20000, 30000, 60, N'ACTIVE'),
    (N'Brake & Chassis Inspection', N'Inspection of brakes, suspension, and undercarriage.', 35000, 45000, 90, N'ACTIVE'),
    (N'Engine Performance Tuning', N'ECU check, configuration review, and performance tuning.', 40000, 60000, 120, N'ACTIVE'),
    (N'Regular Maintenance', N'Oil change, fluid check, filters, and basic maintenance items.', 30000, 45000, 60, N'ACTIVE');

DECLARE @customer_id INT = (SELECT user_id FROM users WHERE email = N'customer@gearshift.local');
DECLARE @bmw_model_id INT = (SELECT TOP 1 car_model_id FROM car_model WHERE brand = N'BMW' AND model_name = N'M4 G82');

IF @customer_id IS NOT NULL AND @bmw_model_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM vehicle WHERE user_id = @customer_id AND license_plate = N'DEMO-302')
BEGIN
    INSERT INTO vehicle (user_id, car_model_id, brand, model_name, [year], license_plate)
    VALUES (@customer_id, @bmw_model_id, N'BMW', N'M4 G82', 2024, N'DEMO-302');
END

INSERT INTO product_car_models (product_id, car_model_id)
SELECT p.product_id, cm.car_model_id
FROM product p
CROSS JOIN car_model cm
WHERE NOT EXISTS (
    SELECT 1 FROM product_car_models existing
    WHERE existing.product_id = p.product_id AND existing.car_model_id = cm.car_model_id
);

PRINT 'CarShowRoom seed script completed.';
