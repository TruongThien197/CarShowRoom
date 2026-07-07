package com.hsf302.carshowroom.config;

import com.hsf302.carshowroom.common.Enums;
import com.hsf302.carshowroom.entity.*;
import com.hsf302.carshowroom.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final VehicleRepository vehicleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        List<Category> categories = seedCategories();
        seedProducts(categories);
        seedDemoOrders();
        seedServices();
        seedCustomerVehicle();
    }

    private void seedUsers() {
        createUserIfMissing("admin@gearshift.local", "Admin GearShift", "ADMIN", "0900000001", "Showroom Office");
        createUserIfMissing("staff@gearshift.local", "Staff GearShift", "STAFF", "0900000002", "Service Bay");
        createUserIfMissing("customer@gearshift.local", "Customer Demo", "CUSTOMER", "0900000003", "123 Demo Street");
    }

    private void createUserIfMissing(String email, String fullName, String role, String phone, String address) {
        if (userRepository.findByEmail(email) != null) {
            return;
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("123456"))
                .fullName(fullName)
                .phone(phone)
                .address(address)
                .role(role)
                .status("ACTIVE")
                .build();
        userRepository.save(user);
    }

    private List<Category> seedCategories() {
        if (categoryRepository.count() == 0) {
            categoryRepository.save(createCategory("Engine Parts", "Turbo, intake, exhaust, and engine performance parts."));
            categoryRepository.save(createCategory("Brakes", "Brake pads, rotors, calipers, and brake upgrade kits."));
            categoryRepository.save(createCategory("Tires & Wheels", "Performance tires, forged wheels, and fitment parts."));
            categoryRepository.save(createCategory("Suspension", "Coilovers, springs, arms, and handling upgrades."));
            categoryRepository.save(createCategory("Oil & Fluids", "Engine oil, coolant, brake fluid, and service fluids."));
        }
        return categoryRepository.findAll();
    }

    private Category createCategory(String name, String description) {
        Category category = new Category();
        category.setCategoryName(name);
        category.setDescription(description);
        return category;
    }

    private void seedProducts(List<Category> categories) {
        if (productRepository.count() > 0 || categories.isEmpty()) {
            return;
        }
        Category engine = findCategory(categories, "Engine Parts");
        Category brakes = findCategory(categories, "Brakes");
        Category wheels = findCategory(categories, "Tires & Wheels");
        Category suspension = findCategory(categories, "Suspension");
        Category fluids = findCategory(categories, "Oil & Fluids");

        productRepository.save(createProduct(engine, "Hybrid Series Turbocharger","SKU001",
                "Direct bolt-on turbo upgrade for high horsepower builds.",
                "2850.00", 8, "/images/turbocharger.jpg"));
        productRepository.save(createProduct(brakes, "Stage 2 Performance Brake Kit","SKU002",
                "Street and track brake kit with upgraded rotors and pads.",
                "1249.99", 12, "/images/suspension-service.jpg"));
        productRepository.save(createProduct(wheels, "Forged Alloy Rims","SKU003",
                "Lightweight satin black forged wheel set.",
                "2450.00", 6, "/images/forged-rims.jpg"));
        productRepository.save(createProduct(wheels, "Track-Ready Tire Set","SKU004",
                "High-grip tire set for daily performance and weekend track use.",
                "1280.00", 16, "/images/track-tire.jpg"));
        productRepository.save(createProduct(suspension, "Track-Spec Coilover Kit","SKU005",
                "Adjustable coilover kit for sharper handling and ride control.",
                "1890.00", 10, "/images/suspension-service.jpg"));
        productRepository.save(createProduct(fluids, "0W-30 Full Synthetic Oil","SKU006",
                "Premium engine oil for modern turbocharged engines.",
                "74.99", 50, "/images/turbocharger.jpg"));
    }

    private Category findCategory(List<Category> categories, String name) {
        return categories.stream()
                .filter(category -> name.equals(category.getCategoryName()))
                .findFirst()
                .orElse(categories.get(0));
    }

    private Product createProduct(Category category, String name, String sku, String description, String price, Integer stock, String imageUrl) {
        Product product = new Product();
        product.setCategory(category);
        product.setName(name);
        product.setSku(sku);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setPhysicalStock(stock);
        product.setImageUrl(imageUrl);
        product.setStatus(Enums.ProductStatus.ACTIVE);
        return product;
    }

    private void seedDemoOrders() {
        if (orderRepository.count() > 0) {
            return;
        }
        User customer = userRepository.findByEmail("customer@gearshift.local");
        List<Product> products = productRepository.findAll();
        if (customer == null || products.isEmpty()) {
            return;
        }

        Enums.OrderStatus[] statuses = {Enums.OrderStatus.PENDING_DEPOSIT, Enums.OrderStatus.PROCESSING, Enums.OrderStatus.SHIPPING, Enums.OrderStatus.COMPLETED, Enums.OrderStatus.CANCELED, Enums.OrderStatus.COMPLETED, Enums.OrderStatus.PENDING_DEPOSIT, Enums.OrderStatus.PROCESSING};
        for (int i = 0; i < statuses.length; i++) {
            Product product = products.get(i % products.size());
            int quantity = (i % 3) + 1;
            BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(quantity));

            Order order = new Order();
            order.setUser(customer);
            order.setCreatedAt(LocalDateTime.now().minus(20L * i, ChronoUnit.DAYS));
            order.setShippingAddress(customer.getAddress() == null ? "123 Demo Street" : customer.getAddress());
            order.setOrderStatus(statuses[i]);
            order.setProductTotal(total);
            order.setOrderType(Enums.OrderType.SHIPPING);
            order.setPaymentStatus(Enums.PaymentStatus.PAID);
            Order savedOrder = orderRepository.save(order);

            OrderItem detail = new OrderItem();
            detail.setOrder(savedOrder);
            detail.setProduct(product);
            detail.setQuantity(quantity);
            detail.setUnitPrice(product.getPrice());
            detail.setLineTotal(total);
            detail.setFulfillmentType(Enums.FulfillmentType.SHIPPING);
            orderItemRepository.save(detail);
        }
    }
    private void seedServices() {
        if (serviceRepository.count() > 0) {
            return;
        }
        serviceRepository.save(createService("Digital Diagnostics", "Full electronic scan and health report.", "120.00", "150.00", 30));
        serviceRepository.save(createService("Brake & Chassis Inspection", "Brake, suspension, and underbody inspection.", "280.00", "350.00", 60));
        serviceRepository.save(createService("Engine Performance Tuning", "ECU check, calibration review, and tuning session.", "450.00", "600.00", 90));
        serviceRepository.save(createService("Regular Maintenance", "Oil, fluids, filters, and general maintenance check.", "180.00", "250.00", 45));
    }

    private com.hsf302.carshowroom.entity.Service createService(String name, String description, String minPrice, String maxPrice, int duration) {
        com.hsf302.carshowroom.entity.Service service = new com.hsf302.carshowroom.entity.Service();
        service.setServiceName(name);
        service.setDescription(description);
        service.setMinPrice(new BigDecimal(minPrice));
        service.setMaxPrice(new BigDecimal(maxPrice));
        service.setDurationMinutes(duration);
        service.setStatus(Enums.ServiceStatus.ACTIVE);
        return service;
    }

    private void seedCustomerVehicle() {
        User customer = userRepository.findByEmail("customer@gearshift.local");
        if (customer == null || !vehicleRepository.findByUser(customer).isEmpty()) {
            return;
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setUser(customer);
        vehicle.setBrand("BMW");
        vehicle.setModelName("M4 G82");
        vehicle.setYear(2024);
        vehicle.setLicensePlate("DEMO-302");
        vehicleRepository.save(vehicle);
    }
}
