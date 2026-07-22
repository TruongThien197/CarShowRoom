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
        payment_method NVARCHAR(20) NULL,
        product_total DECIMAL(18,2) NOT NULL DEFAULT 0,
        shipping_address NVARCHAR(255) NULL,
        receiver_phone NVARCHAR(30) NULL,
        cancellation_reason NVARCHAR(500) NULL,
        shipping_carrier NVARCHAR(100) NULL,
        tracking_code NVARCHAR(100) NULL,
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

IF NOT EXISTS (SELECT 1 FROM category WHERE category_name = N'Phụ tùng động cơ')
    INSERT INTO category (category_name, description) VALUES
    (N'Phụ tùng động cơ', N'Bộ tăng áp, hệ thống nạp, ống xả và phụ tùng hiệu năng động cơ.'),
    (N'Hệ thống phanh', N'Má phanh, đĩa phanh, heo phanh và bộ nâng cấp phanh.'),
    (N'Lốp và mâm xe', N'Lốp hiệu năng, mâm rèn và phụ tùng lắp đặt.'),
    (N'Hệ thống treo', N'Phuộc điều chỉnh, lò xo, tay đòn và phụ tùng tăng khả năng vận hành.'),
    (N'Dầu nhớt và dung dịch', N'Dầu động cơ, nước làm mát, dầu phanh và các dung dịch bảo dưỡng.');

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
    ((SELECT category_id FROM category WHERE category_name = N'Phụ tùng động cơ'), N'Bộ tăng áp Hybrid Series', N'SKU001', N'Bộ tăng áp nâng cấp lắp trực tiếp cho động cơ công suất cao.', 10000, 8, 0, N'/product-images/SKU001.svg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME()),
    ((SELECT category_id FROM category WHERE category_name = N'Hệ thống phanh'), N'Bộ phanh hiệu năng Stage 2', N'SKU002', N'Bộ phanh dùng cho đường phố và đường đua với đĩa và má phanh nâng cấp.', 12000, 12, 0, N'/product-images/SKU002.svg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME()),
    ((SELECT category_id FROM category WHERE category_name = N'Lốp và mâm xe'), N'Mâm hợp kim rèn', N'SKU003', N'Bộ mâm rèn nhẹ với lớp sơn đen mờ.', 14000, 6, 0, N'/product-images/SKU003.svg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME()),
    ((SELECT category_id FROM category WHERE category_name = N'Lốp và mâm xe'), N'Bộ lốp sẵn sàng đường đua', N'SKU004', N'Bộ lốp bám đường cao cho xe sử dụng hằng ngày và cuối tuần đi đường đua.', 16000, 16, 0, N'/product-images/SKU004.svg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME()),
    ((SELECT category_id FROM category WHERE category_name = N'Hệ thống treo'), N'Bộ phuộc điều chỉnh Track-Spec', N'SKU005', N'Bộ phuộc điều chỉnh giúp xe ổn định và lái chính xác hơn.', 18000, 10, 0, N'/product-images/SKU005.svg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME()),
    ((SELECT category_id FROM category WHERE category_name = N'Dầu nhớt và dung dịch'), N'Dầu động cơ tổng hợp 0W-30', N'SKU006', N'Dầu tổng hợp cao cấp cho động cơ hiện đại.', 20000, 50, 0, N'/product-images/SKU006.svg', N'ACTIVE', 0, SYSDATETIME(), SYSDATETIME());
END

IF NOT EXISTS (SELECT 1 FROM service WHERE service_name = N'Chẩn đoán điện tử')
    INSERT INTO service (service_name, description, min_price, max_price, duration_minutes, status) VALUES
    (N'Chẩn đoán điện tử', N'Quét lỗi toàn bộ xe và lập báo cáo tình trạng hệ thống.', 20000, 30000, 60, N'ACTIVE'),
    (N'Kiểm tra phanh và gầm xe', N'Kiểm tra phanh, hệ thống treo và các chi tiết gầm xe.', 35000, 45000, 90, N'ACTIVE'),
    (N'Tinh chỉnh hiệu năng động cơ', N'Kiểm tra ECU, rà soát cấu hình và tinh chỉnh hiệu năng.', 40000, 60000, 120, N'ACTIVE'),
    (N'Bảo dưỡng định kỳ', N'Thay dầu, kiểm tra dung dịch, thay lọc và bảo dưỡng cơ bản.', 30000, 45000, 60, N'ACTIVE');

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
