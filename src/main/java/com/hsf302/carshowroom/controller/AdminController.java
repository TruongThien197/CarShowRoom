package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.common.Enums;
import com.hsf302.carshowroom.dto.CarModelForm;
import com.hsf302.carshowroom.dto.ServiceForm;
import com.hsf302.carshowroom.entity.*;
import com.hsf302.carshowroom.repository.*;
import com.hsf302.carshowroom.service.BookingService;
import com.hsf302.carshowroom.service.FirebaseStorageService;
import com.hsf302.carshowroom.service.OrderService;
import com.hsf302.carshowroom.service.ProductService;
import com.hsf302.carshowroom.service.CategoryService;
import com.hsf302.carshowroom.service.CarModelService;
import com.hsf302.carshowroom.service.ServiceCatalogService;
import com.hsf302.carshowroom.service.UserService;
import com.hsf302.carshowroom.service.AuthService;
import com.hsf302.carshowroom.service.RefundPayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookingRepository bookingRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final CarModelRepository carModelRepository;
    private final ServiceRepository serviceRepository;
    private final OrderService orderService;
    private final BookingService bookingService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final CarModelService carModelService;
    private final ServiceCatalogService serviceCatalogService;
    private final FirebaseStorageService firebaseStorageService;
    private final AuthService authService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final RefundPayoutService refundPayoutService;

    @GetMapping
    public String dashboard(Model model) {
        List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName"));
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Booking> bookings = bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "bookingDate"));
        List<com.hsf302.carshowroom.entity.Service> services = serviceRepository.findAll(Sort.by(Sort.Direction.ASC, "serviceName"));
        BigDecimal orderRevenue = calculateOrderRevenue(orders);
        BigDecimal bookingRevenue = calculateBookingRevenue(bookings);

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("orders", orders);
        model.addAttribute("bookings", bookings);
        model.addAttribute("services", services);
        model.addAttribute("orderRevenue", orderRevenue);
        model.addAttribute("bookingRevenue", bookingRevenue);
        model.addAttribute("totalRevenue", orderRevenue.add(bookingRevenue));
        model.addAttribute("usersCount", userService.getAllUsers().size());
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("totalProducts", products.size());
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("totalBookings", bookings.size());
        model.addAttribute("activeProducts", products.stream().filter(product -> Enums.ProductStatus.ACTIVE.equals(product.getStatus())).count());
        model.addAttribute("pendingOrders", orders.stream().filter(order -> Enums.OrderStatus.PENDING_PAYMENT.equals(order.getOrderStatus())).count());
        model.addAttribute("pendingBookings", bookings.stream().filter(booking -> Enums.BookingStatus.PENDING_PAYMENT.equals(booking.getBookingStatus())).count());
        model.addAttribute("today", LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        model.addAttribute("todayVehicleCount", countTodayVehicles(bookings));
        model.addAttribute("todaySlotLoads", buildTodaySlotLoads(bookings));
        model.addAttribute("monthlyOrderLabels", buildMonthlyOrderLabels());
        model.addAttribute("monthlyOrderCounts", buildMonthlyOrderCounts(orders));
        model.addAttribute("recentOrders", orders.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("recentBookings", bookings.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("orderItems", buildOrderItems(orders));
        model.addAttribute("bookingServices", buildBookingServices(bookings));
        return "admin/dashboard";
    }

    private long countTodayVehicles(List<Booking> bookings) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        return bookings.stream()
                .filter(booking -> today.equals(booking.getBookingDate()))
                .filter(this::isActiveBooking)
                .map(Booking::getVehicle)
                .filter(vehicle -> vehicle != null && vehicle.getId() != null)
                .map(Vehicle::getId)
                .distinct()
                .count();
    }

    private List<SlotLoad> buildTodaySlotLoads(List<Booking> bookings) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        return bookings.stream()
                .filter(booking -> today.equals(booking.getBookingDate()))
                .filter(this::isActiveBooking)
                .collect(Collectors.groupingBy(
                        this::resolveSlotLabel,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new SlotLoad(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(Booking::getVehicle)
                                .filter(vehicle -> vehicle != null && vehicle.getId() != null)
                                .map(Vehicle::getId)
                                .distinct()
                                .count(),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(SlotLoad::timeSlot))
                .collect(Collectors.toList());
    }

    private boolean isActiveBooking(Booking booking) {
        return booking.getBookingStatus() != Enums.BookingStatus.CANCELED;
    }

    private String resolveSlotLabel(Booking booking) {
        if (booking.getTimeSlot() != null && !booking.getTimeSlot().isBlank()) {
            return booking.getTimeSlot();
        }
        if (booking.getStartTime() != null) {
            return booking.getStartTime().toString();
        }
        return "No time yet";
    }

    private record SlotLoad(String timeSlot, long vehicleCount, long bookingCount) {
    }

    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        return "admin/category/list";
    }

    @GetMapping("/car-models")
    public String listCarModels(@RequestParam(required = false) String keyword, Model model) {
        List<CarModel> carModels = carModelRepository.findAllByOrderByBrandAscModelNameAscYearDesc();
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim().toLowerCase();
            carModels = carModels.stream()
                    .filter(carModel -> carModel.getBrand().toLowerCase().contains(normalizedKeyword)
                            || carModel.getModelName().toLowerCase().contains(normalizedKeyword)
                            || String.valueOf(carModel.getYear()).contains(normalizedKeyword))
                    .toList();
        }
        model.addAttribute("carModels", carModels);
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        return "admin/car-model/list";
    }

    @PostMapping("/car-models")
    public String createCarModel(@RequestParam String brand,
                                 @RequestParam String modelName,
                                 @RequestParam Integer year,
                                 RedirectAttributes redirectAttributes) {
        try {
            CarModelForm form = new CarModelForm();
            form.setBrand(brand);
            form.setModelName(modelName);
            form.setYear(year);
            carModelService.createCarModel(form);
            redirectAttributes.addFlashAttribute("successMessage", "Car model added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding car model: " + e.getMessage());
        }
        return "redirect:/admin/car-models";
    }

    @PostMapping("/car-models/delete")
    public String deleteCarModel(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            carModelService.deleteCarModel(id);
            redirectAttributes.addFlashAttribute("successMessage", "Car model deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting car model: " + e.getMessage());
        }
        return "redirect:/admin/car-models";
    }

    @GetMapping("/categories/create")
    public String createCategoryForm() {
        return "admin/category/create";
    }

    @PostMapping("/categories/create")
    public String createCategorySubmit(@RequestParam("categoryName") String categoryName,
                                       @RequestParam(value = "description", required = false) String description,
                                       RedirectAttributes redirectAttributes) {
        try {
            Category category = new Category();
            category.setCategoryName(categoryName);
            category.setDescription(description);
            categoryService.createCategory(category);
            redirectAttributes.addFlashAttribute("successMessage", "Category added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories")
    public String createCategoryQuick(@RequestParam("categoryName") String categoryName,
                                      @RequestParam(value = "description", required = false) String description,
                                      RedirectAttributes redirectAttributes) {
        try {
            Category category = new Category();
            category.setCategoryName(categoryName);
            category.setDescription(description);
            categoryService.createCategory(category);
            redirectAttributes.addFlashAttribute("successMessage", "Category added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategoryForm(@PathVariable Integer id, Model model) {
        model.addAttribute("category", categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục")));
        return "admin/category/edit";
    }

    @PostMapping("/categories/edit/{id}")
    public String editCategorySubmit(@PathVariable Integer id,
                                     @RequestParam("categoryName") String categoryName,
                                     @RequestParam(value = "description", required = false) String description,
                                     RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
            category.setCategoryName(categoryName);
            category.setDescription(description);
            categoryService.updateCategory(id, category);
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting category: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/products")
    public String listProducts(@RequestParam(required = false) Integer categoryId,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer carModelId,
                               @RequestParam(required = false) String brand,
                               @RequestParam(required = false) String modelName,
                               @RequestParam(required = false) Integer year,
                               Model model) {
        List<Product> products = productService.findAdminProductsPaged(categoryId, keyword, carModelId, brand, modelName, year,
                PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "id"))).getContent();
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
        model.addAttribute("carModels", carModelRepository.findAllByOrderByBrandAscModelNameAscYearDesc());
        model.addAttribute("carBrands", carModelRepository.findDistinctBrands());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCarModelId", carModelId);
        model.addAttribute("selectedBrand", brand);
        model.addAttribute("selectedModelName", modelName);
        model.addAttribute("selectedYear", year);
        return "admin/product/list";
    }

    @GetMapping("/products/create")
    public String createProductForm(Model model) {
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
        model.addAttribute("carModels", carModelRepository.findAllByOrderByBrandAscModelNameAscYearDesc());
        return "admin/product/create";
    }

    @PostMapping("/products/create")
    public String createProductSubmit(@RequestParam Integer categoryId,
                                      @RequestParam("productName") String name,
                                      @RequestParam(required = false) String sku,
                                      @RequestParam(required = false) String description,
                                      @RequestParam BigDecimal price,
                                      @RequestParam("stockQuantity") Integer physicalStock,
                                      @RequestParam(required = false) String imageUrl,
                                      @RequestParam(required = false) MultipartFile imageFile,
                                       @RequestParam(required = false) List<Integer> carModelIds,
                                       @RequestParam(defaultValue = "ACTIVE") String status,
                                       RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
            Product product = new Product();
            product.setCategory(category);
            product.setName(name);
            String normalizedSku = (sku == null || sku.isBlank())
                    ? generateSku(name)
                    : sku.trim();
            product.setSku(normalizedSku);
            product.setDescription(description);
            product.setPrice(price);
            product.setPhysicalStock(physicalStock);
            product.setImageUrl(resolveImageUrl(imageFile, imageUrl, null));
            product.setStatus(Enums.ProductStatus.valueOf(status));
            product.getCompatibleCarModels().clear();
            product.getCompatibleCarModels().addAll(resolveCarModels(carModelIds));
            productService.createProduct(product);
            redirectAttributes.addFlashAttribute("successMessage", "Product added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable Integer id, Model model) {
        model.addAttribute("product", productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm")));
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
        model.addAttribute("carModels", carModelRepository.findAllByOrderByBrandAscModelNameAscYearDesc());
        return "admin/product/edit";
    }

    @PostMapping("/products/edit/{id}")
    public String editProductSubmit(@PathVariable Integer id,
                                    @RequestParam Integer categoryId,
                                    @RequestParam("productName") String name,
                                    @RequestParam(required = false) String sku,
                                    @RequestParam(required = false) String description,
                                    @RequestParam BigDecimal price,
                                    @RequestParam("stockQuantity") Integer physicalStock,
                                    @RequestParam(required = false) String imageUrl,
                                    @RequestParam(required = false) MultipartFile imageFile,
                                     @RequestParam(required = false) List<Integer> carModelIds,
                                     @RequestParam(defaultValue = "ACTIVE") String status,
                                     RedirectAttributes redirectAttributes) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
            product.setCategory(category);
            product.setName(name);
            String normalizedSku = (sku == null || sku.isBlank())
                    ? product.getSku()
                    : sku.trim();
            product.setSku(normalizedSku);
            product.setDescription(description);
            product.setPrice(price);
            product.setPhysicalStock(physicalStock);
            product.setImageUrl(resolveImageUrl(imageFile, imageUrl, product.getImageUrl()));
            product.setStatus(Enums.ProductStatus.valueOf(status));
            product.getCompatibleCarModels().clear();
            product.getCompatibleCarModels().addAll(resolveCarModels(carModelIds));
            productService.updateProduct(id, product);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products")
    public String createProduct(@RequestParam Integer categoryId,
                                @RequestParam String name,
                                @RequestParam String sku,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal price,
                                @RequestParam Integer physicalStock,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(required = false) MultipartFile imageFile,
                                 @RequestParam(defaultValue = "ACTIVE") String status,
                                 RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
            Product product = new Product();
            product.setCategory(category);
            product.setName(name);
            product.setSku(sku.trim());
            product.setDescription(description);
            product.setPrice(price);
            product.setPhysicalStock(physicalStock);
            product.setImageUrl(resolveImageUrl(imageFile, imageUrl, null));
            product.setStatus(Enums.ProductStatus.valueOf(status));
            productService.createProduct(product);
            redirectAttributes.addFlashAttribute("successMessage", "Product added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/services")
    public String createService(@RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal minPrice,
                                @RequestParam BigDecimal maxPrice,
                                 @RequestParam int duration,
                                 RedirectAttributes redirectAttributes) {
        try {
            ServiceForm form = new ServiceForm();
            form.setServiceName(name);
            form.setDescription(description);
            form.setMinPrice(minPrice);
            form.setMaxPrice(maxPrice);
            form.setDurationMinutes(duration);
            serviceCatalogService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Service added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding service: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @GetMapping("/services")
    public String listServices(Model model) {
        model.addAttribute("services", serviceRepository.findAll(Sort.by(Sort.Direction.ASC, "serviceName")));
        return "admin/service/list";
    }

    @GetMapping("/services/create")
    public String createServiceForm(Model model) {
        model.addAttribute("serviceForm", new ServiceForm());
        return "admin/service/create";
    }

    @PostMapping("/services/create")
    public String createServiceSubmit(@Valid @ModelAttribute("serviceForm") ServiceForm form,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/service/create";
        }
        try {
            serviceCatalogService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Service added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding service: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @GetMapping("/services/edit/{id}")
    public String editServiceForm(@PathVariable Integer id, Model model) {
        com.hsf302.carshowroom.entity.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));
        ServiceForm form = new ServiceForm();
        form.setServiceName(service.getServiceName());
        form.setDescription(service.getDescription());
        form.setMinPrice(service.getMinPrice());
        form.setMaxPrice(service.getMaxPrice());
        form.setDurationMinutes(service.getDurationMinutes());
        model.addAttribute("service", service);
        model.addAttribute("serviceForm", form);
        return "admin/service/edit";
    }

    @PostMapping("/services/edit/{id}")
    public String editServiceSubmit(@PathVariable Integer id,
                                    @Valid @ModelAttribute("serviceForm") ServiceForm form,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        com.hsf302.carshowroom.entity.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));
        if (bindingResult.hasErrors()) {
            model.addAttribute("service", service);
            return "admin/service/edit";
        }
        try {
            serviceCatalogService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Service updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating service: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/services/delete/{id}")
    public String deleteService(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            serviceCatalogService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Service deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting service: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @GetMapping("/bookings")
    public String listBookings(Model model) {
        List<Booking> bookings = bookingRepository.findAll(Sort.by(Sort.Direction.DESC, "bookingDate"));
        model.addAttribute("bookings", bookings);
        model.addAttribute("bookingServices", buildBookingServices(bookings));
        return "admin/booking/list";
    }

    @GetMapping("/bookings/{id}")
    public String bookingDetail(@PathVariable Integer id, Model model) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));
        model.addAttribute("booking", booking);
        model.addAttribute("bookingServices", bookingServiceRepository.findByBookingId(id));
        model.addAttribute("paymentTransactions", paymentTransactionRepository.findByBooking(booking));
        model.addAttribute("refundTransactions", refundTransactionRepository.findByBookingOrderByCreatedAtDesc(booking));
        return "admin/booking/detail";
    }

    @PostMapping("/products/status")
    public String updateProductStatus(@RequestParam Integer productId, @RequestParam String status,
                                      RedirectAttributes redirectAttributes) {
        try {
            productService.changeStatus(productId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Product status updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/orders/status")
    public String updateOrderStatus(@RequestParam Integer orderId, @RequestParam String status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(orderId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/bookings/status")
    public String updateBookingStatus(@RequestParam Integer bookingId, @RequestParam String status,
                                      RedirectAttributes redirectAttributes) {
        try {
            bookingService.updateStatus(bookingId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Booking status updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/refund")
    public String completeBookingRefund(@RequestParam Integer bookingId,
                                        @RequestParam String bankName,
                                        @RequestParam String bankBin,
                                        @RequestParam String accountHolder,
                                        @RequestParam String accountNumber,
                                        @RequestParam String note,
                                        RedirectAttributes redirectAttributes) {
        try {
            bookingService.completeRefund(bookingId, authService.getCurrentUser(),
                    bankName, bankBin, accountHolder, accountNumber, note);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo lệnh chi PayOS cho hoàn tiền cọc.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings/" + bookingId;
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "role", required = false) String role,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            Model model) {
        int safePage = Math.max(page, 0);
        int pageSize = 10;
        Pageable pageable = PageRequest.of(safePage, pageSize, Sort.by(Sort.Direction.ASC, "id"));
        Page<com.hsf302.carshowroom.entity.User> userPage = userService.searchUsers(keyword, role, status, pageable);

        model.addAttribute("userPage", userPage);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        return "admin/user-list";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("user", userService.getUserByid(id));
        return "admin/user-detail";
    }

    @GetMapping("/users/create")
    public String createUserForm() {
        return "admin/user-create";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam("txtEmail") String email,
                             @RequestParam("txtPassword") String password,
                             @RequestParam("txtFullName") String fullName,
                             @RequestParam(value = "txtPhone", required = false) String phone,
                             @RequestParam(value = "txtAddress", required = false) String address,
                             @RequestParam(defaultValue = "CUSTOMER") String role,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(email, password, fullName, phone, address, role);
            redirectAttributes.addFlashAttribute("successMessage", "User added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Integer id, Model model) {
        model.addAttribute("user", userService.getUserByid(id));
        return "admin/user-edit";
    }

    @PostMapping("/users/edit/{id}")
    public String editUser(@PathVariable Integer id,
                           @RequestParam("txtFullName") String fullName,
                           @RequestParam(value = "txtPhone", required = false) String phone,
                           @RequestParam(value = "txtAddress", required = false) String address,
                           @RequestParam(defaultValue = "CUSTOMER") String role,
                           RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, fullName, phone, address, role);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("users/{id}/change-status")
    public String changeStatus(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            userService.changeStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private Map<Integer, String> buildOrderItems(List<Order> orders) {
        return orders.stream().collect(Collectors.toMap(
                Order::getId,
                order -> orderItemRepository.findByOrderId(order.getId()).stream()
                        .map(detail -> detail.getProduct().getName() + " x" + detail.getQuantity())
                        .collect(Collectors.joining(", "))
        ));
    }

    private Map<Integer, String> buildBookingServices(List<Booking> bookings) {
        return bookings.stream().collect(Collectors.toMap(
                Booking::getId,
                booking -> bookingServiceRepository.findByBookingId(booking.getId()).stream()
                        .map(service -> service.getService() != null
                                ? service.getService().getServiceName()
                                : service.getServiceNameSnapshot())
                        .collect(Collectors.joining(", "))
        ));
    }

    private List<CarModel> resolveCarModels(List<Integer> carModelIds) {
        if (carModelIds == null || carModelIds.isEmpty()) {
            return List.of();
        }
        return carModelRepository.findAllById(carModelIds);
    }

    private String generateSku(String productName) {
        String base = productName == null ? "PART" : productName.trim().toUpperCase()
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "PART";
        }
        return base + "-" + System.currentTimeMillis();
    }

    private String resolveImageUrl(MultipartFile imageFile, String imageUrl, String currentImageUrl) {
        if (imageFile != null && !imageFile.isEmpty()) {
            return firebaseStorageService.uploadProductImage(imageFile);
        }
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl.trim();
        }
        return currentImageUrl;
    }


    private List<String> buildMonthlyOrderLabels() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        List<String> labels = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            labels.add(currentMonth.minusMonths(i).format(formatter));
        }
        return labels;
    }

    private List<Long> buildMonthlyOrderCounts(List<Order> orders) {
        List<Long> counts = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        ZoneId zoneId = ZoneId.systemDefault();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            counts.add(orders.stream()
                    .filter(order -> order.getCreatedAt() != null)
                    .filter(order -> YearMonth.from(order.getCreatedAt().atZone(zoneId)).equals(month))
                    .count());
        }
        return counts;
    }

    private BigDecimal calculateOrderRevenue(List<Order> orders) {
        return orders.stream()
                .filter(order -> Enums.OrderStatus.COMPLETED.equals(order.getOrderStatus()))
                .map(Order::getProductTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateBookingRevenue(List<Booking> bookings) {
        return bookings.stream()
                .filter(booking -> Enums.BookingStatus.COMPLETED.equals(booking.getBookingStatus()))
                .map(booking -> booking.getFinalAmount() != null ? booking.getFinalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @GetMapping("/orders")
    public String listOrders(@RequestParam(value = "status", required = false) String status, Model model) {
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null && !status.isBlank()) {
            orders = orders.stream()
                    .filter(order -> status.equalsIgnoreCase(order.getOrderStatus().name()))
                    .collect(Collectors.toList());
        }
        model.addAttribute("orders", orders);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("orderItems", buildOrderItems(orders));
        return "admin/order/list";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        Order order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("orderDetails", orderItemRepository.findByOrderId(id));
        model.addAttribute("refundTransactions", refundTransactionRepository.findByOrderOrderByCreatedAtDesc(order));
        return "admin/order/detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderDetailStatus(@PathVariable Integer id, @RequestParam String status,
                                          RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/orders/{id}/shipment")
    public String updateOrderShipment(@PathVariable Integer id,
                                      @RequestParam String shippingCarrier,
                                      @RequestParam String trackingCode,
                                      RedirectAttributes redirectAttributes) {
        try {
            orderService.updateShipment(id, shippingCarrier, trackingCode);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin vận chuyển.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/orders/{id}/refund")
    public String completeOrderRefund(@PathVariable Integer id,
                                      @RequestParam String bankName,
                                      @RequestParam String bankBin,
                                      @RequestParam String accountHolder,
                                      @RequestParam String accountNumber,
                                      @RequestParam String note,
                                      RedirectAttributes redirectAttributes) {
        try {
            orderService.completeRefund(id, authService.getCurrentUser(), bankName, bankBin, accountHolder, accountNumber, note);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo lệnh chi PayOS cho hoàn tiền.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/refunds/{id}/sync")
    public String syncRefund(@PathVariable Long id,
                             @RequestParam String returnUrl,
                             RedirectAttributes redirectAttributes) {
        try {
            refundPayoutService.syncRefundTransaction(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đồng bộ trạng thái lệnh chi PayOS.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:" + safeLocalReturnUrl(returnUrl);
    }

    private String safeLocalReturnUrl(String returnUrl) {
        return returnUrl != null && returnUrl.startsWith("/") && !returnUrl.startsWith("//") ? returnUrl : "/admin";
    }
}
