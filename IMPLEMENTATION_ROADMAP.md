# IMPLEMENTATION ROADMAP - Chi tiết từng bước

> Dựa trên SRS GearShift Pro và phân tích hiện trạng dự án

---

## PHASE 1 - CRITICAL FEATURES (Tuần 1)

### 1.1 Thêm Fulfillment Type vào CartItem

**File cần sửa:** `src/main/java/com/hsf302/carshowroom/entity/CartItem.java`

```java
// Thêm field vào CartItem entity
@Enumerated(EnumType.STRING)
@Column(name = "fulfillment_type", nullable = false)
private FulfillmentType fulfillmentType; // SHIPPING hoặc AT_WORKSHOP

// Thêm getter/setter (Lombok @Getter @Setter sẽ auto gen)
```

**File cần sửa:** `src/main/java/com/hsf302/carshowroom/common/Enums.java`

```java
// Thêm enum nếu chưa có
public enum FulfillmentType {
    SHIPPING,      // Giao tận nơi
    AT_WORKSHOP    // Lắp tại xưởng
}
```

**File cần sửa:** `src/main/java/com/hsf302/carshowroom/controller/CartController.java`

```java
// Modify addToCart() endpoint để bắt fulfillmentType
@PostMapping("/add")
public String add(@RequestParam Integer productId,
                  @RequestParam(defaultValue = "1") Integer quantity,
                  @RequestParam FulfillmentType fulfillmentType,  // ADD THIS
                  RedirectAttributes redirectAttributes) {
    // ...
    cartService.addToCart(user, productId, quantity, fulfillmentType);
    // ...
}
```

**File cần sửa:** `src/main/java/com/hsf302/carshowroom/service/CartService.java`

```java
// Update signature
void addToCart(User user, Integer productId, Integer quantity, FulfillmentType fulfillmentType);
```

**File cần sửa:** `src/main/java/com/hsf302/carshowroom/service/impl/CartServiceImpl.java`

```java
@Override
@Transactional
public void addToCart(User user, Integer productId, Integer quantity, FulfillmentType fulfillmentType) {
    // ... existing validation ...
    CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product)
            .orElseGet(() -> {
                CartItem item = new CartItem();
                item.setUser(user);
                item.setProduct(product);
                item.setQuantity(0);
                item.setFulfillmentType(fulfillmentType);  // SET HERE
                return item;
            });
    // ... rest of logic
}
```

---

### 1.2 Implement Order Splitting Logic

**File cần tạo:** `src/main/java/com/hsf302/carshowroom/service/CheckoutService.java`

```java
package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.*;
import com.hsf302.carshowroom.common.Enums.*;
import java.util.*;

public interface CheckoutService {
    Order checkout(User user, CheckoutForm form);
}
```

**File cần tạo:** `src/main/java/com/hsf302/carshowroom/service/impl/CheckoutServiceImpl.java`

```java
package com.hsf302.carshowroom.service.impl;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckoutServiceImpl implements CheckoutService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final InventoryReservationService inventoryReservationService;

    @Override
    public Order checkout(User user, CheckoutForm form) {
        // 1. Get cart items
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống");
        }

        // 2. Categorize by fulfillment type
        List<CartItem> shippingItems = cartItems.stream()
            .filter(item -> item.getFulfillmentType() == FulfillmentType.SHIPPING)
            .collect(Collectors.toList());
        
        List<CartItem> workshopItems = cartItems.stream()
            .filter(item -> item.getFulfillmentType() == FulfillmentType.AT_WORKSHOP)
            .collect(Collectors.toList());

        // 3. Determine order type and create order(s)
        Order result;
        
        if (!shippingItems.isEmpty() && !workshopItems.isEmpty()) {
            // MIXED CART - Create parent + 2 sub-orders
            result = createMixedOrders(user, form, shippingItems, workshopItems);
        } else if (!shippingItems.isEmpty()) {
            // SHIPPING ONLY
            result = createShippingOrder(user, form, shippingItems);
        } else if (!workshopItems.isEmpty()) {
            // AT_WORKSHOP ONLY
            result = createWorkshopOrder(user, form, workshopItems);
        } else {
            throw new RuntimeException("Không có item hợp lệ");
        }

        // 4. Clear cart
        cartItemRepository.deleteByUser(user);
        
        return result;
    }

    private Order createShippingOrder(User user, CheckoutForm form, List<CartItem> items) {
        // Validate stock
        BigDecimal productTotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            Product product = item.getProduct();
            if (!validateAddtStock(product, item.getQuantity())) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " không đủ kho");
            }
            productTotal = productTotal.add(
                product.getPrice().multiply(new BigDecimal(item.getQuantity()))
            );
        }

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setOrderType(OrderType.SHIPPING);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingAddress(form.getShippingAddress());
        order.setProductTotal(productTotal);
        
        // Calculate deposit (20%)
        BigDecimal depositAmount = productTotal.multiply(new BigDecimal("0.2"));
        order.setDepositAmount(depositAmount);
        order.setRemainingAmount(productTotal.subtract(depositAmount));
        
        Order savedOrder = orderRepository.save(order);

        // Create order items
        for (CartItem cartItem : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getProduct().getPrice());
            orderItemRepository.save(orderItem);
        }

        // Reserve inventory (tạm giữ 15 phút)
        for (CartItem cartItem : items) {
            inventoryReservationService.reserve(
                cartItem.getProduct().getId(),
                savedOrder.getId(),
                null, // bookingId
                cartItem.getQuantity(),
                15 // minutes
            );
        }

        // Create payment link
        PaymentTransaction transaction = paymentService.createPaymentLink(
            savedOrder.getId(),
            null, // bookingId
            depositAmount
        );

        // Update order status
        order.setOrderStatus(OrderStatus.PENDING_DEPOSIT);
        orderRepository.save(order);

        return savedOrder;
    }

    private Order createWorkshopOrder(User user, CheckoutForm form, List<CartItem> items) {
        // Similar logic but must create booking
        // TODO: Implement
        return null;
    }

    private Order createMixedOrders(User user, CheckoutForm form, 
            List<CartItem> shippingItems, List<CartItem> workshopItems) {
        // 1. Create parent order
        // 2. Create shipping sub-order
        // 3. Create workshop sub-order
        // 4. Create booking
        // 5. Reserve and create payment for all
        // TODO: Implement
        return null;
    }

    private boolean validateAddtStock(Product product, int quantity) {
        int available = product.getPhysicalStock() - product.getReservedStock();
        return available >= quantity;
    }
}
```

---

### 1.3 Implement Slot Scheduling Engine

**File cần tạo:** `src/main/java/com/hsf302/carshowroom/service/impl/SchedulingServiceImpl.java`

```java
package com.hsf302.carshowroom.service.impl;

@Service
@RequiredArgsConstructor
public class SchedulingServiceImpl implements SchedulingService {
    private final BookingRepository bookingRepository;

    private static final int WORKING_HOURS_START = 8;   // 8 AM
    private static final int WORKING_HOURS_END = 18;    // 6 PM
    private static final int WORKSHOP_CAPACITY = 3;     // 3 bays
    private static final int SLOT_INTERVAL_MINUTES = 15;

    @Override
    public List<AvailableSlotDTO> findAvailableSlots(
        LocalDate bookingDate,
        int totalDurationMinutes,
        Long vehicleId) {
        
        List<AvailableSlotDTO> slots = new ArrayList<>();
        
        // Get existing bookings for that day
        List<Booking> existingBookings = bookingRepository.findByBookingDate(bookingDate)
            .stream()
            .filter(b -> b.getBookingStatus().isOccupyingCapacity())
            .collect(Collectors.toList());
        
        // Generate candidate slots
        LocalTime currentTime = LocalTime.of(WORKING_HOURS_START, 0);
        LocalTime endOfDay = LocalTime.of(WORKING_HOURS_END, 0);
        
        while (currentTime.plus(totalDurationMinutes, ChronoUnit.MINUTES).isBefore(endOfDay)) {
            LocalTime candidateEnd = currentTime.plus(totalDurationMinutes, ChronoUnit.MINUTES);
            
            // Check if this slot is available
            int overlappingBookings = (int) existingBookings.stream()
                .filter(b -> isOverlap(currentTime, candidateEnd, b.getStartTime(), b.getEndTime()))
                .count();
            
            if (overlappingBookings < WORKSHOP_CAPACITY) {
                slots.add(new AvailableSlotDTO(
                    bookingDate,
                    currentTime,
                    candidateEnd,
                    WORKSHOP_CAPACITY - overlappingBookings
                ));
            }
            
            currentTime = currentTime.plus(SLOT_INTERVAL_MINUTES, ChronoUnit.MINUTES);
        }
        
        return slots;
    }

    private boolean isOverlap(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        return s1.isBefore(e2) && e1.isAfter(s2);
    }
}
```

---

### 1.4 PayOS Integration - Payment Link Creation

**File cần sửa:** `src/main/java/com/hsf302/carshowroom/service/impl/PaymentServiceImpl.java`

```java
package com.hsf302.carshowroom.service.impl;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RestTemplate restTemplate; // Cần config
    
    @Value("${payos.client.id}")
    private String payosClientId;
    
    @Value("${payos.api.key}")
    private String payosApiKey;
    
    @Value("${payos.checksum.key}")
    private String payosChecksumKey;

    @Override
    public PaymentTransaction createPaymentLink(
        Long orderId,
        Long bookingId,
        BigDecimal depositAmount) {
        
        // Create transaction
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId(orderId);
        transaction.setBookingId(bookingId);
        transaction.setAmount(depositAmount);
        transaction.setStatus(PaymentStatus.INITIATED);
        transaction.setCreatedAt(LocalDateTime.now());
        
        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);
        
        // Call PayOS API
        try {
            PayOSCreatePaymentLinkRequest payosRequest = new PayOSCreatePaymentLinkRequest();
            payosRequest.setOrderCode(String.valueOf(savedTransaction.getId()));
            payosRequest.setAmount(depositAmount.longValue() * 100); // PayOS uses cents
            payosRequest.setDescription("Đặt cọc đơn hàng/lịch hẹn");
            payosRequest.setReturnUrl("https://yourdomain/checkout/success");
            payosRequest.setCancelUrl("https://yourdomain/checkout/cancel");
            
            // Call PayOS (pseudocode - replace with actual API)
            String checkoutUrl = callPayOSAPI(payosRequest);
            
            // Save checkout URL and order code
            transaction.setPayosOrderCode(payosRequest.getOrderCode());
            transaction.setCheckoutUrl(checkoutUrl);
            transaction.setStatus(PaymentStatus.PENDING);
            transaction.setPaymentDeadline(LocalDateTime.now().plus(15, ChronoUnit.MINUTES));
            
            paymentTransactionRepository.save(transaction);
            
            return transaction;
        } catch (Exception e) {
            transaction.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            throw new RuntimeException("Tạo link thanh toán thất bại: " + e.getMessage());
        }
    }

    private String callPayOSAPI(PayOSCreatePaymentLinkRequest request) {
        // TODO: Implement actual PayOS API call
        return "https://payos.vn/checkout/..." ;
    }
}
```

---

### 1.5 PayOS Webhook Endpoint

**File cần tạo:** `src/main/java/com/hsf302/carshowroom/controller/PayOSWebhookController.java`

```java
package com.hsf302.carshowroom.controller;

@RestController
@RequestMapping("/api/payos")
@RequiredArgsConstructor
public class PayOSWebhookController {
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    
    @Value("${payos.checksum.key}")
    private String payosChecksumKey;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handlePayOSWebhook(@RequestBody PayOSWebhookRequest request) {
        try {
            // 1. Verify signature
            if (!verifyPayOSSignature(request)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 2. Find transaction
            PaymentTransaction transaction = paymentTransactionRepository
                .findByPayosOrderCode(request.getOrderCode())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
            
            // 3. Check if already processed (idempotency)
            if (transaction.getStatus() == PaymentStatus.PAID) {
                return ResponseEntity.ok().build();
            }
            
            // 4. Update transaction status
            if ("PAID".equals(request.getStatus())) {
                transaction.setStatus(PaymentStatus.PAID);
                transaction.setPaidAt(LocalDateTime.now());
                
                // 5. Update related order/booking
                if (transaction.getOrderId() != null) {
                    Order order = orderRepository.findById(transaction.getOrderId())
                        .orElseThrow();
                    order.setOrderStatus(OrderStatus.DEPOSITED);
                    order.setPaymentStatus(PaymentStatus.PAID);
                    orderRepository.save(order);
                }
                
                if (transaction.getBookingId() != null) {
                    Booking booking = bookingRepository.findById(transaction.getBookingId())
                        .orElseThrow();
                    booking.setBookingStatus(BookingStatus.CONFIRMED);
                    bookingRepository.save(booking);
                }
                
                // 6. Confirm inventory reservation
                paymentService.confirmReservation(transaction.getId());
                
            } else if ("FAILED".equals(request.getStatus()) || 
                       "CANCELED".equals(request.getStatus())) {
                transaction.setStatus(PaymentStatus.FAILED);
                
                // Release reservations
                paymentService.releaseReservation(transaction.getId());
                
                // Cancel orders/bookings if needed
                // ...
            }
            
            paymentTransactionRepository.save(transaction);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            // Log error but return OK to PayOS
            return ResponseEntity.ok().build();
        }
    }

    private boolean verifyPayOSSignature(PayOSWebhookRequest request) {
        // TODO: Implement PayOS signature verification
        return true; // Placeholder
    }
}
```

---

### 1.6 Inventory Reservation Service

**File cần sửa:** `src/main/java/com/hsf302/carshowroom/service/impl/InventoryReservationServiceImpl.java`

```java
package com.hsf302.carshowroom.service.impl;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryReservationServiceImpl implements InventoryReservationService {
    private final InventoryReservationRepository reservationRepository;
    private final ProductRepository productRepository;

    @Override
    public InventoryReservation reserve(Long productId, Long orderId, Long bookingId, 
                                        int quantity, int expirationMinutes) {
        // 1. Validate stock
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        int available = product.getPhysicalStock() - product.getReservedStock();
        if (available < quantity) {
            throw new RuntimeException("Không đủ hàng trong kho");
        }
        
        // 2. Create reservation
        InventoryReservation reservation = new InventoryReservation();
        reservation.setProductId(productId);
        reservation.setOrderId(orderId);
        reservation.setBookingId(bookingId);
        reservation.setQuantity(quantity);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setExpiresAt(LocalDateTime.now().plus(expirationMinutes, ChronoUnit.MINUTES));
        
        InventoryReservation saved = reservationRepository.save(reservation);
        
        // 3. Update product reserved stock
        product.setReservedStock(product.getReservedStock() + quantity);
        productRepository.save(product);
        
        return saved;
    }

    @Override
    public void confirm(Long reservationId) {
        InventoryReservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow();
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
    }

    @Override
    public void release(Long reservationId) {
        InventoryReservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow();
        
        // Update product stock
        Product product = productRepository.findById(reservation.getProductId()).orElseThrow();
        product.setReservedStock(product.getReservedStock() - reservation.getQuantity());
        productRepository.save(product);
        
        // Update reservation
        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);
    }

    @Override
    public void consume(Long reservationId) {
        InventoryReservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow();
        
        // Update product stock
        Product product = productRepository.findById(reservation.getProductId()).orElseThrow();
        product.setPhysicalStock(product.getPhysicalStock() - reservation.getQuantity());
        product.setReservedStock(product.getReservedStock() - reservation.getQuantity());
        productRepository.save(product);
        
        // Update reservation
        reservation.setStatus(ReservationStatus.CONSUMED);
        reservationRepository.save(reservation);
    }
}
```

---

## PHASE 2 - HIGH PRIORITY FEATURES (Tuần 2)

### 2.1 Implement Deposit Calculation Complete

**File cần tạo:** `src/main/java/com/hsf302/carshowroom/service/DepositCalculationService.java`

```java
package com.hsf302.carshowroom.service;

public interface DepositCalculationService {
    BigDecimal calculateOrderDeposit(List<CartItem> items);
    BigDecimal calculateBookingDeposit(List<Service> services);
    BigDecimal calculateMixedDeposit(List<CartItem> productItems, List<Service> services);
}
```

---

### 2.2 Audit ID Spoofing Prevention

Thêm vào tất cả endpoints quan trọng:

```java
@PreAuthorize("hasRole('CUSTOMER')")
@GetMapping("/{id}")
public String detail(@PathVariable Long id, Model model) {
    User currentUser = authService.getCurrentUser();
    Object object = repository.findById(id).orElseThrow();
    
    // Verify ownership
    if (!object.getUser().getId().equals(currentUser.getId())) {
        throw new AccessDeniedException("Không có quyền truy cập");
    }
    
    // ...
}
```

---

### 2.3 State Machine Enforcement

**File cần tạo:** `src/main/java/com/hsf302/carshowroom/service/OrderStateValidator.java`

```java
@Component
public class OrderStateValidator {
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.ofEntries(
        Map.entry(OrderStatus.CREATED, Set.of(OrderStatus.PENDING_DEPOSIT, OrderStatus.CANCELED)),
        Map.entry(OrderStatus.PENDING_DEPOSIT, Set.of(OrderStatus.DEPOSITED, OrderStatus.EXPIRED_PAYMENT, OrderStatus.CANCELED)),
        Map.entry(OrderStatus.DEPOSITED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELED)),
        Map.entry(OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPING, OrderStatus.COMPLETED, OrderStatus.CANCELED)),
        Map.entry(OrderStatus.SHIPPING, Set.of(OrderStatus.COMPLETED)),
        Map.entry(OrderStatus.COMPLETED, Set.of()),
        Map.entry(OrderStatus.CANCELED, Set.of()),
        Map.entry(OrderStatus.EXPIRED_PAYMENT, Set.of())
    );
    
    public void validateTransition(OrderStatus from, OrderStatus to) {
        if (!VALID_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidOrderStateException(
                String.format("Không thể chuyển từ %s sang %s", from, to)
            );
        }
    }
}
```

---

## Database Migrations Needed

```sql
-- Add fulfillment_type to cart_items
ALTER TABLE cart_items ADD fulfillment_type VARCHAR(20);

-- Add payment_deadline to bookings  
ALTER TABLE bookings ADD payment_deadline DATETIME;

-- Make sure inventory_reservations has proper columns
-- (Should already exist based on entity)

-- Add working_hours configuration table
CREATE TABLE workshop_working_hours (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    day_of_week INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL
);

-- Add workshop capacity configuration
CREATE TABLE workshop_settings (
    id BIGINT PRIMARY KEY,
    capacity INT NOT NULL,
    max_daily_bookings INT NOT NULL
);
```

---

## Configuration Needed (application.properties)

```properties
# PayOS Configuration
payos.client.id=your_client_id
payos.api.key=your_api_key
payos.checksum.key=your_checksum_key
payos.api.url=https://api.sandbox.payos.vn

# Deposit percentage
app.deposit.percentage=0.2

# Reservation timeout (minutes)
app.reservation.timeout=15

# Workshop settings
app.workshop.capacity=3
app.workshop.working.hours.start=8
app.workshop.working.hours.end=18
```

---

## Testing Strategy

### Unit Tests
- DepositCalculationService
- SchedulingService slot finding
- Order splitting logic

### Integration Tests  
- Checkout flow: SHIPPING only
- Checkout flow: WORKSHOP only
- Checkout flow: MIXED cart
- PayOS webhook handling

### E2E Tests
- Full user journey: Browse → Cart → Checkout → Payment → Confirmation

---

## Deployment Checklist

- [ ] Database schema updated
- [ ] PayOS sandbox account configured
- [ ] Environment variables set
- [ ] Tests passing (Unit + Integration)
- [ ] Performance tested under load
- [ ] Security review (SQL injection, XSS, CSRF)
- [ ] Webhook endpoint accessible
- [ ] Backup & recovery plan

---

*Estimated Timeline: 3-4 weeks to complete all phases*

