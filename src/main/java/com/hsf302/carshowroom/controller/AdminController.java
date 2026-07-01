package com.hsf302.carshowroom.controller;

import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Category;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.repository.BookingDetailRepository;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.CategoryRepository;
import com.hsf302.carshowroom.repository.OrderDetailRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
        model.addAttribute("activeProducts", products.stream().filter(product -> "ACTIVE".equalsIgnoreCase(product.getStatus())).count());
        model.addAttribute("pendingOrders", orders.stream().filter(order -> "PENDING".equalsIgnoreCase(order.getStatus())).count());
        model.addAttribute("pendingBookings", bookings.stream().filter(booking -> "PENDING".equalsIgnoreCase(booking.getStatus())).count());
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
                                       @RequestParam(value = "description", required = false) String description) {
        Category category = new Category();
        category.setCategoryName(categoryName);
        category.setDescription(description);
        categoryRepository.save(category);
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories")
    public String createCategoryQuick(@RequestParam("categoryName") String categoryName,
                                      @RequestParam(value = "description", required = false) String description) {
        Category category = new Category();
        category.setCategoryName(categoryName);
        category.setDescription(description);
        categoryRepository.save(category);
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
                                     @RequestParam(value = "description", required = false) String description) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        category.setCategoryName(categoryName);
        category.setDescription(description);
        categoryRepository.save(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Integer id) {
        categoryRepository.deleteById(id);
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
                                      @RequestParam(defaultValue = "ACTIVE") String status) {
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
                                    @RequestParam(defaultValue = "ACTIVE") String status) {
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
        return "redirect:/admin/products";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Integer id) {
        productRepository.deleteById(id);
        return "redirect:/admin/products";
    }

    @PostMapping("/products")
    public String createProduct(@RequestParam Integer categoryId,
                                @RequestParam String productName,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal price,
                                @RequestParam Integer stockQuantity,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(defaultValue = "ACTIVE") String status) {
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
        return "redirect:/admin#products";
    }

    @PostMapping("/services")
    public String createService(@RequestParam String serviceName,
                                @RequestParam(required = false) String description,
                                @RequestParam BigDecimal price) {
        com.hsf302.carshowroom.entity.Service service = new com.hsf302.carshowroom.entity.Service();
        service.setServiceName(serviceName);
        service.setDescription(description);
        service.setPrice(price);
        serviceRepository.save(service);
        return "redirect:/admin#services";
    }

    @PostMapping("/products/status")
    public String updateProductStatus(@RequestParam Integer productId, @RequestParam String status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStatus(status);
        productRepository.save(product);
        return "redirect:/admin#products";
    }

    @PostMapping("/orders/status")
    public String updateOrderStatus(@RequestParam Integer orderId, @RequestParam String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
        return "redirect:/admin#orders";
    }

    @PostMapping("/bookings/status")
    public String updateBookingStatus(@RequestParam Integer bookingId, @RequestParam String status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(status);
        bookingRepository.save(booking);
        return "redirect:/admin#bookings";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
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
                             @RequestParam(defaultValue = "CUSTOMER") String role) {
        userService.createUser(email, password, fullName, phone, address, role);
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
                           @RequestParam(defaultValue = "CUSTOMER") String role) {
        userService.updateUser(id, fullName, phone, address, role);
        return "redirect:/admin/users";
    }

    @GetMapping("users/{id}/change-status")
    public String changeStatus(@PathVariable Integer id) {
        userService.changeStatus(id);
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
}
