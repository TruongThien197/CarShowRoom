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
            categoryRepository.save(createCategory("Engine", "Turbochargers, air filters, spark plugs, exhausts, and engine performance parts."));
            categoryRepository.save(createCategory("Brakes", "Brake pads, rotors, calipers, and brake upgrade kits."));
            categoryRepository.save(createCategory("Tires & Wheels", "Performance tires, forged rims, and alignment accessories."));
            categoryRepository.save(createCategory("Suspension", "Coilovers, springs, sway bars, and parts to improve handling stability."));
            categoryRepository.save(createCategory("Oil & Fluids", "Engine oil, coolant, brake fluid, and maintenance fluids."));
            categoryRepository.save(createCategory("Electrical & Sensors", "Sensors, batteries, lights, and vehicle electrical accessories."));
            categoryRepository.save(createCategory("Interior", "Cabin accessories, floor mats, seats, and trim details."));
            categoryRepository.save(createCategory("Car Care", "Wash solutions, protective coatings, towels, and detailing tools."));
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
        Category engine = findCategory(categories, "Engine");
        Category brakes = findCategory(categories, "Brakes");
        Category wheels = findCategory(categories, "Tires & Wheels");
        Category suspension = findCategory(categories, "Suspension");
        Category fluids = findCategory(categories, "Oil & Fluids");

        productRepository.save(createProduct(engine, "Hybrid Series Turbocharger Kit","SKU001",
                "A direct-fit upgrade turbo kit built for high-output engine setups.",
                "10000", 8, "/images/turbocharger.jpg"));
        productRepository.save(createProduct(brakes, "Stage 2 Performance Brake Kit","SKU002",
                "A street and track brake kit featuring upgraded rotors and pads.",
                "12000", 12, "/images/suspension-service.jpg"));
        productRepository.save(createProduct(wheels, "Forged Alloy Rims","SKU003",
                "Lightweight forged rims finished in satin black.",
                "14000", 6, "/images/forged-rims.jpg"));
        productRepository.save(createProduct(wheels, "Track-Ready Tire Set","SKU004",
                "High-grip tires built for daily-driven and weekend performance cars.",
                "16000", 16, "/images/track-tire.jpg"));
        productRepository.save(createProduct(suspension, "Track-Spec Coilover Kit","SKU005",
                "An adjustable coilover kit that improves handling stability and driving feel.",
                "18000", 10, "/images/suspension-service.jpg"));
        productRepository.save(createProduct(fluids, "0W-30 Full Synthetic Oil","SKU006",
                "Premium engine oil formulated for modern turbocharged engines.",
                "20000", 50, "/images/turbocharger.jpg"));
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

        Enums.OrderStatus[] statuses = {Enums.OrderStatus.PENDING_PAYMENT, Enums.OrderStatus.PROCESSING, Enums.OrderStatus.SHIPPING, Enums.OrderStatus.COMPLETED, Enums.OrderStatus.CANCELED, Enums.OrderStatus.COMPLETED, Enums.OrderStatus.PENDING_PAYMENT, Enums.OrderStatus.PROCESSING};
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
            detail.setProductNameSnapshot(product.getName());
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
        serviceRepository.save(createService("Digital Diagnostics", "A full vehicle error scan with a system health report.", "20000", "30000", 30));
        serviceRepository.save(createService("Brake & Chassis Inspection", "Inspection of brakes, suspension, and undercarriage components.", "35000", "45000", 60));
        serviceRepository.save(createService("Engine Performance Tuning", "ECU inspection, configuration review, and performance tuning.", "40000", "60000", 90));
        serviceRepository.save(createService("Regular Maintenance", "Oil change, fluid check, filter replacement, and basic maintenance items.", "30000", "45000", 45));
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
