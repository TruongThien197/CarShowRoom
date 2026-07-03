package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Category;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.dto.ServiceForm;
import com.hsf302.carshowroom.repository.BookingDetailRepository;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.CategoryRepository;
import com.hsf302.carshowroom.repository.OrderDetailRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.service.OrderService;
import com.hsf302.carshowroom.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final OrderDetailRepository orderDetailRepository;
    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ServiceRepository serviceRepository;
    private  final OrderService orderService;

    @GetMapping
    public String dashboard(Model model) {
        List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.ASC, "productName"));
        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName"));
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"));
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
        model.addAttribute("activeProducts", products.stream().filter(product -> "ACTIVE".equalsIgnoreCase(product.getStatus())).count());
        model.addAttribute("pendingOrders", orders.stream().filter(order -> "PENDING".equalsIgnoreCase(order.getStatus())).count());
        model.addAttribute("pendingBookings", bookings.stream().filter(booking -> "PENDING".equalsIgnoreCase(booking.getStatus())).count());
        model.addAttribute("monthlyOrderLabels", buildMonthlyOrderLabels());
        model.addAttribute("monthlyOrderCounts", buildMonthlyOrderCounts(orders));
        model.addAttribute("recentOrders", orders.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("recentBookings", bookings.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("orderItems", buildOrderItems(orders));
        model.addAttribute("bookingServices", buildBookingServices(bookings));
        return "admin/dashboard";
    }

    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        return "admin/category/list";
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
            categoryRepository.save(category);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi thêm danh mục: " + e.getMessage());
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
            categoryRepository.save(category);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi thêm danh mục: " + e.getMessage());
        }
        return "redirect:/admin#products";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategoryForm(@PathVariable Integer id, Model model) {
        model.addAttribute("category", categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found")));
        return "admin/category/edit";
    }

    @PostMapping("/categories/edit/{id}")
    public String editCategorySubmit(@PathVariable Integer id,
                                     @RequestParam("categoryName") String categoryName,
                                     @RequestParam(value = "description", required = false) String description,
                                     RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            category.setCategoryName(categoryName);
            category.setDescription(description);
            categoryRepository.save(category);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi cập nhật danh mục: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi xóa danh mục: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
        return "admin/product/list";
    }

    @GetMapping("/products/create")
    public String createProductForm(Model model) {
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
        return "admin/product/create";
    }

    @PostMapping("/products/create")
    public String createProductSubmit(@RequestParam Integer categoryId,
                                      @RequestParam String productName,
                                      @RequestParam(required = false) String description,
                                      @RequestParam BigDecimal price,
                                      @RequestParam Integer stockQuantity,
                                      @RequestParam(required = false) String imageUrl,
                                      @RequestParam(defaultValue = "ACTIVE") String status,
                                      RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            Product product = new Product();
            product.setCategory(category);
            product.setProductName(productName);
            product.setDescription(description);
            product.setPrice(price);
            product.setStockQuantity(stockQuantity);
            product.setImageUrl(imageUrl);
            product.setStatus(status);
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi thêm sản phẩm: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable Integer id, Model model) {
        model.addAttribute("product", productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found")));
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
        return "admin/product/edit";
    }

    @PostMapping("/products/edit/{id}")
    public String editProductSubmit(@PathVariable Integer id,
                                    @RequestParam Integer categoryId,
                                    @RequestParam String productName,
                                    @RequestParam(required = false) String description,
                                    @RequestParam BigDecimal price,
                                    @RequestParam Integer stockQuantity,
                                    @RequestParam(required = false) String imageUrl,
                                    @RequestParam(defaultValue = "ACTIVE") String status,
                                    RedirectAttributes redirectAttributes) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
            product.setProductName(productName);
            product.setDescription(description);
            product.setPrice(price);
            product.setStockQuantity(stockQuantity);
            product.setImageUrl(imageUrl);
            product.setStatus(status);
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi cập nhật sản phẩm: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            productRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products")
    public String createProduct(@RequestParam Integer categoryId,
                                @RequestParam String productName,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal price,
                                @RequestParam Integer stockQuantity,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(defaultValue = "ACTIVE") String status,
                                RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            Product product = new Product();
            product.setCategory(category);
            product.setProductName(productName);
            product.setDescription(description);
            product.setPrice(price);
            product.setStockQuantity(stockQuantity);
            product.setImageUrl(imageUrl);
            product.setStatus(status);
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi thêm sản phẩm: " + e.getMessage());
        }
        return "redirect:/admin#products";
    }

    @PostMapping("/services")
    public String createService(@RequestParam String serviceName,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal price,
                                RedirectAttributes redirectAttributes) {
        try {
            com.hsf302.carshowroom.entity.Service service = new com.hsf302.carshowroom.entity.Service();
            service.setServiceName(serviceName);
            service.setDescription(description);
            service.setPrice(price);
            serviceRepository.save(service);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi thêm dịch vụ: " + e.getMessage());
        }
        return "redirect:/admin#services";
    }

    @GetMapping("/services")
    public String listServices(Model model) {
        model.addAttribute("services", serviceRepository.findAllByOrderByServiceNameAsc());
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
            com.hsf302.carshowroom.entity.Service service = new com.hsf302.carshowroom.entity.Service();
            fillService(service, form);
            serviceRepository.save(service);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi thêm dịch vụ: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @GetMapping("/services/edit/{id}")
    public String editServiceForm(@PathVariable Integer id, Model model) {
        com.hsf302.carshowroom.entity.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        ServiceForm form = new ServiceForm();
        form.setServiceName(service.getServiceName());
        form.setDescription(service.getDescription());
        form.setPrice(service.getPrice());
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
                .orElseThrow(() -> new RuntimeException("Service not found"));
        if (bindingResult.hasErrors()) {
            model.addAttribute("service", service);
            return "admin/service/edit";
        }
        try {
            fillService(service, form);
            serviceRepository.save(service);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi cập nhật dịch vụ: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @GetMapping("/services/delete/{id}")
    public String deleteService(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            serviceRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi xóa dịch vụ: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @GetMapping("/bookings")
    public String listBookings(Model model) {
        List<Booking> bookings = bookingRepository.findAllByOrderByBookingDateDesc();
        model.addAttribute("bookings", bookings);
        model.addAttribute("bookingServices", buildBookingServices(bookings));
        return "admin/booking/list";
    }

    @GetMapping("/bookings/{id}")
    public String bookingDetail(@PathVariable Integer id, Model model) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        model.addAttribute("booking", booking);
        model.addAttribute("bookingDetails", bookingDetailRepository.findByBookingId(id));
        return "admin/booking/detail";
    }

    @PostMapping("/products/status")
    public String updateProductStatus(@RequestParam Integer productId, @RequestParam String status,
                                      RedirectAttributes redirectAttributes) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            product.setStatus(status);
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/admin#products";
    }

    @PostMapping("/orders/status")
    public String updateOrderStatus(@RequestParam Integer orderId, @RequestParam String status,
                                    RedirectAttributes redirectAttributes) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            order.setStatus(status);
            orderRepository.save(order);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/admin#orders";
    }

    @PostMapping("/bookings/status")
    public String updateBookingStatus(@RequestParam Integer bookingId, @RequestParam String status,
                                      RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            booking.setStatus(status);
            bookingRepository.save(booking);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái lịch hẹn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/admin#bookings";
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
            redirectAttributes.addFlashAttribute("successMessage", "Thêm người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi thêm người dùng: " + e.getMessage());
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
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi cập nhật người dùng: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("users/{id}/change-status")
    public String changeStatus(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            userService.changeStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private Map<Integer, String> buildOrderItems(List<Order> orders) {
        return orders.stream().collect(Collectors.toMap(
                Order::getId,
                order -> orderDetailRepository.findByOrderId(order.getId()).stream()
                        .map(detail -> detail.getProduct().getProductName() + " x" + detail.getQuantity())
                        .collect(Collectors.joining(", "))
        ));
    }

    private Map<Integer, String> buildBookingServices(List<Booking> bookings) {
        return bookings.stream().collect(Collectors.toMap(
                Booking::getId,
                booking -> bookingDetailRepository.findByBookingId(booking.getId()).stream()
                        .map(detail -> detail.getService().getServiceName())
                        .collect(Collectors.joining(", "))
        ));
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
                    .filter(order -> order.getOrderDate() != null)
                    .filter(order -> YearMonth.from(order.getOrderDate().atZone(zoneId)).equals(month))
                    .count());
        }
        return counts;
    }

    private BigDecimal calculateOrderRevenue(List<Order> orders) {
        return orders.stream()
                .filter(order -> "DELIVERED".equalsIgnoreCase(order.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateBookingRevenue(List<Booking> bookings) {
        return bookings.stream()
                .filter(booking -> "COMPLETED".equalsIgnoreCase(booking.getStatus()))
                .flatMap(booking -> bookingDetailRepository.findByBookingId(booking.getId()).stream())
                .map(detail -> detail.getActualPrice() == null ? BigDecimal.ZERO : detail.getActualPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void fillService(com.hsf302.carshowroom.entity.Service service, ServiceForm form) {
        service.setServiceName(form.getServiceName());
        service.setDescription(form.getDescription());
        service.setPrice(form.getPrice());
    }
    @GetMapping("/orders")
    public String listOrders(@RequestParam(value = "status", required = false) String status, Model model) {
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"));
        if (status != null && !status.isBlank()) {
            orders = orders.stream()
                    .filter(order -> status.equalsIgnoreCase(order.getStatus()))
                    .collect(Collectors.toList());
        }
        model.addAttribute("orders", orders);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("orderItems", buildOrderItems(orders));
        return "admin/order/list";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("order", orderService.getOrderById(id));
        model.addAttribute("orderDetails", orderDetailRepository.findByOrderId(id));
        return "admin/order/detail";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderDetailStatus(@PathVariable Integer id, @RequestParam String status,
                                          RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
