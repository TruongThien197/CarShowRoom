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
    private final ShippingFeeRuleRepository shippingFeeRuleRepository;


    @Override
    public void run(String... args) {
        seedUsers();
        List<Category> categories = seedCategories();
        seedProducts(categories);
        seedDemoOrders();
        seedServices();
        seedCustomerVehicle();
//        seedShippingFeeRules();
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
        createCategoryIfMissing("Động cơ", "Bộ tăng áp, lọc gió, bugi, hệ thống xả và các phụ tùng nâng cao hiệu suất động cơ.");
        createCategoryIfMissing("Phanh", "Má phanh, đĩa phanh, cùm phanh và bộ nâng cấp hệ thống phanh.");
        createCategoryIfMissing("Lốp & Mâm", "Lốp hiệu suất cao, mâm xe và các phụ kiện cân chỉnh bánh xe.");
        createCategoryIfMissing("Hệ thống treo", "Coilover, lò xo, thanh cân bằng và các bộ phận cải thiện khả năng vận hành.");
        createCategoryIfMissing("Dầu nhớt & Dung dịch", "Dầu động cơ, nước làm mát, dầu phanh và các dung dịch bảo dưỡng xe.");
        createCategoryIfMissing("Điện & Cảm biến", "Cảm biến, ắc quy, đèn chiếu sáng và các phụ kiện điện ô tô.");
        createCategoryIfMissing("Nội thất", "Phụ kiện khoang lái, thảm sàn, ghế ngồi và các chi tiết trang trí nội thất.");
        createCategoryIfMissing("Chăm sóc xe", "Dung dịch rửa xe, lớp phủ bảo vệ, khăn lau và dụng cụ chăm sóc xe.");
        return categoryRepository.findAll();
    }

    private void createCategoryIfMissing(String name, String description) {
        if (categoryRepository.findByCategoryNameIgnoreCase(name) == null) {
            categoryRepository.save(createCategory(name, description));
        }
    }

    private Category createCategory(String name, String description) {
        Category category = new Category();
        category.setCategoryName(name);
        category.setDescription(description);
        return category;
    }

    private void seedProducts(List<Category> categories) {
        if (categories.isEmpty()) {
            return;
        }

        Category engine = findCategory(categories, "Động cơ");
        Category brakes = findCategory(categories, "Phanh");
        Category wheels = findCategory(categories, "Lốp & Mâm");
        Category suspension = findCategory(categories, "Hệ thống treo");
        Category fluids = findCategory(categories, "Dầu nhớt & Dung dịch");


        seedProductIfMissing(createProduct(
                engine,
                "Bộ Turbo Hybrid Series",
                "SKU001",
                "Bộ tăng áp lắp đặt trực tiếp, giúp nâng cao hiệu suất động cơ.",
                "10000",
                8,
                "/images/turbocharger.jpg"));

        seedProductIfMissing(createProduct(
                brakes,
                "Bộ Phanh Hiệu Suất Stage 2",
                "SKU002",
                "Bộ phanh nâng cấp gồm đĩa phanh và má phanh dành cho cả đường phố và đường đua.",
                "12000",
                12,
                "/images/brake_discs_phanh.jpg"));

        seedProductIfMissing(createProduct(
                wheels,
                "Mâm Hợp Kim Rèn",
                "SKU003",
                "Mâm xe hợp kim rèn trọng lượng nhẹ với lớp sơn đen nhám.",
                "14000",
                6,
                "/images/mam_hop_kim.jpg"));

        seedProductIfMissing(createProduct(
                wheels,
                "Bộ Lốp Hiệu Suất Cao",
                "SKU004",
                "Bộ lốp bám đường tốt, phù hợp cho xe sử dụng hằng ngày và xe hiệu suất.",
                "16000",
                16,
                "/images/wheel.jpg"));

        seedProductIfMissing(createProduct(
                suspension,
                "Bộ Coilover Track-Spec",
                "SKU005",
                "Bộ giảm xóc có thể điều chỉnh, cải thiện khả năng vận hành và độ ổn định.",
                "18000",
                10,
                "/images/SKU005.jpg"));

        seedProductIfMissing(createProduct(
                fluids,
                "Dầu Động Cơ Tổng Hợp 0W-30",
                "SKU006",
                "Dầu nhớt tổng hợp cao cấp dành cho động cơ tăng áp hiện đại.",
                "20000",
                50,
                "/product-images/auth-garage.jpg"));

    }
    private void seedProductIfMissing(Product product) {
        if (productRepository.findBySkuIgnoreCase(product.getSku()) == null) {
            productRepository.save(product);
        }
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
    /**
     * Khởi tạo dữ liệu dịch vụ mẫu và luôn bảo đảm có dịch vụ cố định
     * "Thay thế phụ tùng" cho luồng đặt hàng lắp tại xưởng.
     */
    private void seedServices() {
        if (serviceRepository.count() == 0) {
            serviceRepository.save(createService("Chẩn đoán kỹ thuật số", "Quét lỗi toàn bộ xe và lập báo cáo tình trạng hệ thống.", "20000", "30000", 30));
            serviceRepository.save(createService("Kiểm tra phanh và gầm xe", "Kiểm tra hệ thống phanh, giảm xóc và các chi tiết dưới gầm.", "35000", "45000", 60));
            serviceRepository.save(createService("Tinh chỉnh hiệu suất động cơ", "Kiểm tra ECU, rà soát cấu hình và tinh chỉnh hiệu suất.", "40000", "60000", 90));
            serviceRepository.save(createService("Bảo dưỡng định kỳ", "Thay dầu, kiểm tra dung dịch, thay lọc và bảo dưỡng cơ bản.", "30000", "45000", 45));
        }
        serviceRepository.findFirstByServiceNameIgnoreCase("Thay thế phụ tùng")
                .ifPresentOrElse(service -> {
                    if (service.getMinPrice().compareTo(service.getMaxPrice()) == 0
                            && "Lắp đặt phụ tùng khách đã đặt tại xưởng.".equals(service.getDescription())) {
                        service.setMaxPrice(new BigDecimal("150000"));
                        serviceRepository.save(service);
                    }
                }, () -> serviceRepository.save(createService(
                        "Thay thế phụ tùng",
                        "Lắp đặt phụ tùng khách đã đặt tại xưởng.",
                        "50000", "150000", 45)));
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

//    private void saveShippingRule(String province, String district, int fee) {
//        ShippingFeeRule rule = new ShippingFeeRule();
//
//        rule.setProvince(province);
//        rule.setDistrict(district);
//        rule.setFee(BigDecimal.valueOf(fee));
//        rule.setActive(true);
//
//        shippingFeeRuleRepository.save(rule);
//    }

//    private void seedShippingFeeRules() {
//        if (shippingFeeRuleRepository.count() > 0) {
//            return;
//        }
//
//        // Hà Nội
//        saveShippingRule("Hà Nội", "Ba Đình", 30000);
//        saveShippingRule("Hà Nội", "Cầu Giấy", 30000);
//        saveShippingRule("Hà Nội", "Đống Đa", 35000);
//
//        // TP Hồ Chí Minh
//        saveShippingRule("TP. Hồ Chí Minh", "Quận 1", 30000);
//        saveShippingRule("TP. Hồ Chí Minh", "Quận 3", 30000);
//        saveShippingRule("TP. Hồ Chí Minh", "Quận 7", 35000);
//        saveShippingRule("TP. Hồ Chí Minh", "Thủ Đức", 40000);
//
//        // Đà Nẵng
//        saveShippingRule("Đà Nẵng", "Hải Châu", 30000);
//        saveShippingRule("Đà Nẵng", "Thanh Khê", 30000);
//        saveShippingRule("Đà Nẵng", "Sơn Trà", 35000);
//
//        // Cần Thơ
//        saveShippingRule("Cần Thơ", "Ninh Kiều", 35000);
//        saveShippingRule("Cần Thơ", "Bình Thủy", 35000);
//
//        // Hải Phòng
//        saveShippingRule("Hải Phòng", "Hồng Bàng", 35000);
//        saveShippingRule("Hải Phòng", "Ngô Quyền", 35000);
//    }
}
