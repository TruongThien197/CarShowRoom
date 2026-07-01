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
        status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
    );
END
GO

IF OBJECT_ID(N'category', N'U') IS NULL
BEGIN
    CREATE TABLE category (
        category_id INT IDENTITY(1,1) PRIMARY KEY,
        category_name NVARCHAR(100) NOT NULL,
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
        category_id INT NOT NULL,
        product_name NVARCHAR(150) NOT NULL,
        description NVARCHAR(MAX) NULL,
        price DECIMAL(18,2) NOT NULL,
        stock_quantity INT NOT NULL DEFAULT 0,
        image_url NVARCHAR(500) NULL,
        status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
        CONSTRAINT FK_product_category FOREIGN KEY (category_id) REFERENCES category(category_id)
    );
END
GO

IF OBJECT_ID(N'service', N'U') IS NULL
BEGIN
    CREATE TABLE service (
        service_id INT IDENTITY(1,1) PRIMARY KEY,
        service_name NVARCHAR(150) NOT NULL,
        description NVARCHAR(MAX) NULL,
        price DECIMAL(18,2) NOT NULL
    );
END
GO

IF OBJECT_ID(N'vehicle', N'U') IS NULL
BEGIN
    CREATE TABLE vehicle (
        vehicle_id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        car_model_id INT NOT NULL,
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
        CONSTRAINT FK_cart_item_user FOREIGN KEY (user_id) REFERENCES users(user_id),
        CONSTRAINT FK_cart_item_product FOREIGN KEY (product_id) REFERENCES product(product_id)
    );
END
GO

IF OBJECT_ID(N'orders', N'U') IS NULL
BEGIN
    CREATE TABLE orders (
        order_id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        order_date DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        total_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
        shipping_address NVARCHAR(255) NOT NULL,
        status NVARCHAR(50) NOT NULL DEFAULT 'PENDING',
        CONSTRAINT FK_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id)
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

IF OBJECT_ID(N'booking', N'U') IS NULL
BEGIN
    CREATE TABLE booking (
        booking_id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        vehicle_id INT NULL,
        booking_date DATE NOT NULL,
        time_slot NVARCHAR(50) NOT NULL,
        status NVARCHAR(50) NOT NULL DEFAULT 'PENDING',
        notes NVARCHAR(MAX) NULL,
        CONSTRAINT FK_booking_user FOREIGN KEY (user_id) REFERENCES users(user_id),
        CONSTRAINT FK_booking_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id)
    );
END
GO

IF OBJECT_ID(N'booking', N'U') IS NOT NULL AND COL_LENGTH(N'booking', N'user_id') IS NULL
BEGIN
    ALTER TABLE booking ADD user_id INT NULL;
END
GO

IF OBJECT_ID(N'booking', N'U') IS NOT NULL AND COL_LENGTH(N'booking', N'vehicle_id') IS NULL
BEGIN
    ALTER TABLE booking ADD vehicle_id INT NULL;
END
GO

IF OBJECT_ID(N'FK_booking_user', N'F') IS NULL AND OBJECT_ID(N'booking', N'U') IS NOT NULL
BEGIN
    ALTER TABLE booking ADD CONSTRAINT FK_booking_user FOREIGN KEY (user_id) REFERENCES users(user_id);
END
GO

IF OBJECT_ID(N'FK_booking_vehicle', N'F') IS NULL AND OBJECT_ID(N'booking', N'U') IS NOT NULL
BEGIN
    ALTER TABLE booking ADD CONSTRAINT FK_booking_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id);
END
GO

IF OBJECT_ID(N'booking_detail', N'U') IS NULL
BEGIN
    CREATE TABLE booking_detail (
        booking_detail_id INT IDENTITY(1,1) PRIMARY KEY,
        booking_id INT NOT NULL,
        service_id INT NOT NULL,
        actual_price DECIMAL(18,2) NOT NULL,
        CONSTRAINT FK_booking_detail_booking FOREIGN KEY (booking_id) REFERENCES booking(booking_id) ON DELETE CASCADE,
        CONSTRAINT FK_booking_detail_service FOREIGN KEY (service_id) REFERENCES service(service_id)
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

DECLARE @password_hash NVARCHAR(255) = N'$2a$10$O3OGQjdH.WH.LHpGBMF6Pux36EG1ewV54RpXH0QoLn91827QZo.Ai';

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
    (N'Toyota', N'Supra GR', 2022);

IF NOT EXISTS (SELECT 1 FROM product WHERE product_name = N'Hybrid Series Turbocharger')
BEGIN
    INSERT INTO product (category_id, product_name, description, price, stock_quantity, image_url, status)
    VALUES
    ((SELECT category_id FROM category WHERE category_name = N'Engine Parts'), N'Hybrid Series Turbocharger', N'Direct bolt-on turbo upgrade for high horsepower builds.', 2850.00, 8, N'/images/turbocharger.jpg', N'ACTIVE'),
    ((SELECT category_id FROM category WHERE category_name = N'Brakes'), N'Stage 2 Performance Brake Kit', N'Street and track brake kit with upgraded rotors and pads.', 1249.99, 12, N'/images/suspension-service.jpg', N'ACTIVE'),
    ((SELECT category_id FROM category WHERE category_name = N'Tires & Wheels'), N'Forged Alloy Rims', N'Lightweight satin black forged wheel set.', 2450.00, 6, N'/images/forged-rims.jpg', N'ACTIVE'),
    ((SELECT category_id FROM category WHERE category_name = N'Tires & Wheels'), N'Track-Ready Tire Set', N'High-grip tire set for daily performance and weekend track use.', 1280.00, 16, N'/images/track-tire.jpg', N'ACTIVE'),
    ((SELECT category_id FROM category WHERE category_name = N'Suspension'), N'Track-Spec Coilover Kit', N'Adjustable coilover kit for sharper handling and ride control.', 1890.00, 10, N'/images/suspension-service.jpg', N'ACTIVE'),
    ((SELECT category_id FROM category WHERE category_name = N'Oil & Fluids'), N'0W-30 Full Synthetic Oil', N'Premium engine oil for modern turbocharged engines.', 74.99, 50, N'/images/turbocharger.jpg', N'ACTIVE');
END

IF NOT EXISTS (SELECT 1 FROM service WHERE service_name = N'Digital Diagnostics')
    INSERT INTO service (service_name, description, price) VALUES
    (N'Digital Diagnostics', N'Full electronic scan and health report.', 120.00),
    (N'Brake & Chassis Inspection', N'Brake, suspension, and underbody inspection.', 280.00),
    (N'Engine Performance Tuning', N'ECU check, calibration review, and tuning session.', 450.00),
    (N'Regular Maintenance', N'Oil, fluids, filters, and general maintenance check.', 180.00);

DECLARE @customer_id INT = (SELECT user_id FROM users WHERE email = N'customer@gearshift.local');
DECLARE @bmw_model_id INT = (SELECT TOP 1 car_model_id FROM car_model WHERE brand = N'BMW' AND model_name = N'M4 G82');

IF @customer_id IS NOT NULL AND @bmw_model_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM vehicle WHERE user_id = @customer_id AND license_plate = N'DEMO-302')
BEGIN
    INSERT INTO vehicle (user_id, car_model_id, license_plate)
    VALUES (@customer_id, @bmw_model_id, N'DEMO-302');
END

DECLARE @vehicle_id INT = (SELECT TOP 1 vehicle_id FROM vehicle WHERE user_id = @customer_id AND license_plate = N'DEMO-302');
DECLARE @service_id INT = (SELECT TOP 1 service_id FROM service WHERE service_name = N'Brake & Chassis Inspection');

IF @customer_id IS NOT NULL AND @service_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM booking WHERE user_id = @customer_id AND booking_date = CAST(DATEADD(DAY, 2, GETDATE()) AS DATE))
BEGIN
    INSERT INTO booking (user_id, vehicle_id, booking_date, time_slot, status, notes)
    VALUES (@customer_id, @vehicle_id, CAST(DATEADD(DAY, 2, GETDATE()) AS DATE), N'09:30', N'PENDING', N'Customer reports brake noise.');

    INSERT INTO booking_detail (booking_id, service_id, actual_price)
    VALUES (SCOPE_IDENTITY(), @service_id, (SELECT price FROM service WHERE service_id = @service_id));
END

DECLARE @turbo_id INT = (SELECT TOP 1 product_id FROM product WHERE product_name = N'Hybrid Series Turbocharger');
DECLARE @oil_id INT = (SELECT TOP 1 product_id FROM product WHERE product_name = N'0W-30 Full Synthetic Oil');

IF @customer_id IS NOT NULL AND @turbo_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM orders WHERE user_id = @customer_id AND shipping_address = N'123 Demo Street')
BEGIN
    INSERT INTO orders (user_id, order_date, total_amount, shipping_address, status)
    VALUES (@customer_id, SYSDATETIME(), 2924.99, N'123 Demo Street', N'PENDING');

    DECLARE @order_id INT = SCOPE_IDENTITY();

    INSERT INTO order_detail (order_id, product_id, quantity, unit_price)
    VALUES
    (@order_id, @turbo_id, 1, (SELECT price FROM product WHERE product_id = @turbo_id)),
    (@order_id, @oil_id, 1, (SELECT price FROM product WHERE product_id = @oil_id));
END

PRINT 'CarShowRoom seed script completed.';
