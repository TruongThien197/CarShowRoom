# Kiểm tra Tuân thủ Yêu cầu Nghiệp vụ - GearShift Pro CarShowRoom
**Ngày kiểm tra:** 2026-07-06  
**Dự án:** CarShowRoom (D:\FPT\SUMMER_2026\HSF302\CarShowRoom)

---

## TÓNG THƯ TRÍCH ĐÁNH GIÁ

### ✅ ĐÃ TRIỂN KHAI (Khoảng 60%)

#### 1. **Cấu trúc dữ liệu cơ bản** ✅
- ✅ Entity User, Product, Service, Booking, Order, CartItem
- ✅ Order với parent/sub-order structure
- ✅ PaymentTransaction entity
- ✅ InventoryReservation entity
- ✅ Booking entity với status enum (BookingStatus)
- ✅ Order với status enum (OrderStatus, PaymentStatus)
- ✅ Service với min_price, max_price, duration_minutes

#### 2. **Phân quyền cơ bản** ✅
- ✅ User role: CUSTOMER, STAFF, ADMIN
- ✅ SecurityConfig cấu hình cơ bản
- ✅ AuthService/AuthServiceImpl
- ✅ AuthController (login/register)

#### 3. **Hàng hóa - Danh sách** ✅
- ✅ ProductController - xem danh sách sản phẩm
- ✅ AdminController - CRUD sản phẩm
- ✅ Product entity với status (ProductStatus enum)

#### 4. **Dịch vụ - Danh sách** ✅
- ✅ ServiceRepository
- ✅ AdminController - CRUD dịch vụ
- ✅ Service entity với min_price, max_price, duration_minutes

#### 5. **Giỏ hàng** ✅
- ✅ CartService interface
- ✅ CartServiceImpl implementation
- ✅ CartItem entity
- ✅ CartItemRepository
- ✅ CartController (add/update/remove)

#### 6. **Tính năng xe** ✅
- ✅ Vehicle entity
- ✅ VehicleService/VehicleServiceImpl
- ✅ VehicleController
- ✅ CarModel entity

#### 7. **Lịch hẹn cơ bản** ✅
- ✅ BookingService interface
- ✅ BookingServiceImpl implementation
- ✅ BookingController
- ✅ Booking entity với booking_status, booking_date, start_time, end_time
- ✅ BookingDetail entity

---

## ❌ CHƯA TRIỂN KHAI HOẶC CẦN CẢI TIẾN (Khoảng 40%)

### A. **LỖI & THIẾU SÓ NGHIÊM TRỌNG**

#### 1. ❌ **Luồng Fulfillment Type chưa xây dựng**
**Yêu cầu SRS:** FR-CART-001, FR-ORDER-001,002,003
- Mỗi CartItem phải có `fulfillment_type` (SHIPPING hoặc AT_WORKSHOP)
- Khi checkout, hệ thống phải tách order thành SHIPPING / AT_WORKSHOP / Parent Order

**Hiện tại:** 
- ❌ CartItem không có fulfillment_type field
- ❌ Không có logic tách order (order splitting)
- ❌ Luồng order luôn tạo một order duy nhất

**Cần làm:**
```java
// CartItem cần thêm field
@Enumerated(EnumType.STRING)
private FulfillmentType fulfillmentType; // SHIPPING, AT_WORKSHOP

// Order cần field này để distinguish
@Enumerated(EnumType.STRING)
private OrderType orderType; // SHIPPING, AT_WORKSHOP, PRODUCT_ONLY, MIXED (đã có)
```

---

#### 2. ❌ **Kho không được giữ khi tạo payment link (PENDING_DEPOSIT)**
**Yêu cầu SRS:** FR-INV-002, 3.1, 3.2
- Khi thanh toán ở PENDING_DEPOSIT, hệ thống phải:
  - Tạm giữ kho trong 15 phút
  - Nếu thanh toán thành công → giữ chính thức
  - Nếu hết hạn → giải phóng kho

**Hiện tại:**
- ❌ Không có logic tạm giữ kho khi PENDING_DEPOSIT
- ❌ Không có cơ chế timeout 15 phút
- ❌ OrderServiceImpl.checkout() giảm stock trực tiếp mà không check PENDING_DEPOSIT state

**Cần làm:**
```java
// Trong OrderServiceImpl.checkout()
// KHÔNG nên gọi product.setPhysicalStock(product.getPhysicalStock() - quantity) ngay lập tức
// Thay vào đó, chỉ reserve khi chuyển sang PENDING_DEPOSIT:
// reserved_stock += quantity
// Khi webhook PAID → confirm, khi FAILED/timeout → release
```

---

#### 3. ❌ **Booking không giữ slot khi PENDING_DEPOSIT**
**Yêu cầu SRS:** 3.2, FR-SLOT-004
- Khi booking ở PENDING_DEPOSIT, slot phải được tính là chiếm capacity
- Nếu hết hạn thanh toán → giải phóng slot

**Hiện tại:**
- ❌ Không có payment_deadline trên Booking
- ❌ Không có logic check timeout
- ❌ SchedulingService/Slot algorithm chưa implement

**Cần làm:**
```java
// Booking cần thêm field
@Column(name = "payment_deadline")
private LocalDateTime paymentDeadline; // Này đã có trong code

// Booking status cần xác định khi nào chiếm capacity
// Theo SRS: PENDING_DEPOSIT, CONFIRMED, IN_PROGRESS chiếm
//           CANCELED, EXPIRED_PAYMENT không chiếm
```

---

#### 4. ❌ **Slot Scheduling Engine chưa implement**
**Yêu cầu SRS:** FR-SLOT-001,002,003,004
- Tính tổng thời lượng từ nhiều service
- Sinh slot động dựa vào working_hours, capacity, existing bookings
- Kiểm tra overlap booking

**Hiện tại:**
- ❌ Không có logic sinh slot
- ❌ SchedulingService interface tồn tại nhưng chưa implement
- ❌ Chưa có bảng working_hours, capacity trong database

**Cần làm:**
- Implement SchedulingService với algorithm tìm slot hợp lệ
- Thêm bảng WorkingHours hoặc cấu hình
- Implement overlap detection

---

#### 5. ❌ **Deposit Calculation chưa hoàn toàn**
**Yêu cầu SRS:** FR-PAY-001,002,003
- Cọc đơn phụ tùng: `product_total * 20%`
- Cọc lịch hẹn: `service_min_total * 20%`
- Cọc O2O: `(shipping_product + workshop_product + service_min) * 20%`

**Hiện tại:**
- ⚠️ OrderServiceImpl.checkout() tính `subtotal` nhưng không tính cọc
- ❌ Booking deposit không được tính
- ❌ Luồng O2O mixed chưa hỗ trợ

**Cần làm:**
```java
// Trong checkout hoặc payment service
BigDecimal depositAmount = productTotal.multiply(new BigDecimal("0.2")); // 20%
order.setDepositAmount(depositAmount);
order.setRemainingAmount(productTotal.subtract(depositAmount));
```

---

#### 6. ❌ **PayOS Integration chưa hoàn chỉnh**
**Yêu cầu SRS:** FR-PAYOS-001,002,003
- Tạo payment link khi checkout
- Return URL chỉ hiển thị UI (không dùng làm bằng chứng)
- Webhook xác minh checksum, idempotent, cập nhật PaymentTransaction trước

**Hiện tại:**
- ⚠️ PaymentTransaction entity tồn tại
- ⚠️ PaymentService interface tồn tại
- ❌ Không thấy PayOS API integration
- ❌ Webhook endpoint không implement
- ❌ PayOS checksum verification không có

**Cần làm:**
- Tích hợp PayOS API (tạo payment link)
- Implement webhook endpoint POST /api/payos/webhook
- Verify PayOS signature
- Implement idempotent logic

---

#### 7. ❌ **Inventory Reservation System chưa hoàn toàn**
**Yêu cầu SRS:** FR-INV-001-006
- InventoryReservationService interface tồn tại nhưng chưa implement
- Không có logic release reservation khi timeout
- Không có optimistic locking conflict retry

**Hiện tại:**
- ⚠️ InventoryReservation entity tồn tại
- ❌ InventoryReservationService impl chưa code
- ❌ Không tích hợp với order/booking workflow

**Cần làm:**
- Implement InventoryReservationService đầy đủ
- Thêm @Version trên Product để optimistic lock
- Implement reserve/confirm/release/consume workflows

---

#### 8. ❌ **Order Splitting (Mixed Cart) chưa implement**
**Yêu cầu SRS:** FR-ORDER-003, 2.2
- Khi giỏ có cả SHIPPING và AT_WORKSHOP:
  - Tạo Parent Order
  - Tạo Sub-Order SHIPPING
  - Tạo Sub-Order AT_WORKSHOP
  - Tạo Booking tương ứng
  - Tính cọc tổng

**Hiện tại:**
- ❌ Checkout logic chỉ xử lý single order type
- ❌ Không có logic phân tích cart items
- ⚠️ Order entity có parent/sub structure nhưng chưa dùng

**Cần làm:**
```java
// OrderServiceImpl.checkout() cần:
// 1. Phân loại cart items theo fulfillmentType
// 2. Nếu chỉ SHIPPING → tạo order SHIPPING
// 3. Nếu chỉ AT_WORKSHOP → tạo order AT_WORKSHOP + booking
// 4. Nếu cả hai → tạo parent order + 2 sub-orders + booking
// 5. Tính cọc chung cho tất cả
```

---

#### 9. ❌ **Validate Ownership & ID Spoofing**
**Yêu cầu SRS:** FR-AUTH-004
- Customer chỉ được xem booking nếu `booking.user.id == currentUser.id`
- Customer chỉ được sửa order/booking của chính mình

**Hiện tại:**
- ⚠️ Một số controller check ownership (VehicleController)
- ❌ BookingController getBookingDetail() có check, nhưng không đầy đủ
- ❌ OrderController không check ownership

**Cần làm:**
- Thêm @PreAuthorize hoặc manual check trong mọi endpoint
- Middleware hoặc Service layer validate ownership

---

#### 10. ❌ **Webhook Idempotency & Security**
**Yêu cầu SRS:** FR-PAYOS-003
- Webhook phải check xem transaction đã PAID chưa
- Nếu webhook trùng → không cộng/trừ kho nhiều lần
- Phải verify PayOS signature

**Hiện tại:**
- ❌ Không thấy webhook endpoint
- ❌ Không có verify checksum

**Cần làm:**
```java
// Pseudo webhook
@PostMapping("/api/payos/webhook")
public ResponseEntity<Void> handlePayOSWebhook(@RequestBody PayOSWebhookRequest request) {
    // 1. Verify signature
    if (!verifyPayOSSignature(request)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    // 2. Find transaction by payos_order_code
    PaymentTransaction transaction = paymentTransactionRepo.findByPayosOrderCode(...);
    if (transaction.getStatus() == PaymentStatus.PAID) {
        return ResponseEntity.ok().build(); // Already processed
    }
    
    // 3. Update transaction
    transaction.setStatus(PaymentStatus.PAID);
    
    // 4. Update order/booking
    order.setOrderStatus(OrderStatus.DEPOSITED);
    
    // 5. Confirm reservation
    // ...
    
    return ResponseEntity.ok().build();
}
```

---

### B. **CẦN CẢI TIẾN (Tính năng kỹ thuật)**

#### 11. ⚠️ **Concurrent Update Issue**
**Yêu cầu SRS:** FR-INV-006
- Không có @Version optimization locking trên Product

**Cần làm:**
```java
@Entity
public class Product {
    @Version
    private Long version; // Đã có
    // ...
}
```

---

#### 12. ⚠️ **Transactional Safety**
**Yêu cầu SRS:** 3.1, 3.2, 3.3
- Checkout, reserve, confirm, release phải transaction-safe
- Không có error handling khi reserve conflict

**Cần làm:**
- Thêm @Transactional trên các service method
- Implement retry logic
- Error handling tốt

---

#### 13. ⚠️ **State Machine Enforcement**
**Yêu cầu SRS:** Section 5 (State Machines)
- Không có enforce state transitions
- Có thể từ bất kỳ state nào sang bất kỳ state nào

**Cần làm:**
- Implement state machine logic
- Valid transitions:
  ```
  CREATED → PENDING_DEPOSIT → DEPOSITED → PROCESSING → SHIPPING/COMPLETED
         → CANCELED
         → EXPIRED_PAYMENT
  ```

---

#### 14. ⚠️ **Booking Status Transitions**
**Yêu cầu SRS:** Section 5.2
- BookingStatus enum phải enforce transitions theo SRS
- Chưa implement IN_PROGRESS, PENDING_APPROVAL states

---

#### 15. ⚠️ **Vehicle Management - Incomplete**
**Yêu cầu SRS:** FR-VEH-001
- ⚠️ VehicleController tồn tại nhưng có thể chưa đầy đủ
- ⚠️ Cần validate số km >= 0

---

### C. **CHƯA THẤY IMPLEMENT (Features)**

#### 16. ❌ **Business Intelligence Dashboard**
**Yêu cầu SRS:** Admin FR - Xem báo cáo, BI dashboard
- Không thấy endpoint/view cho:
  - Doanh thu cọc
  - Dự báo dòng tiền
  - Tỷ lệ lấp đầy xưởng
  - Tồn kho đang giữ chỗ

---

#### 17. ❌ **Notification System**
**Yêu cầu SRS:** Customer FR - Xem thông báo nội bộ
- ⚠️ Notification entity tồn tại
- ❌ Notification service/controller chưa thấy
- ❌ Không có webhook trigger notification

---

#### 18. ❌ **Hủy Đơn & Hoàn Tiền**
**Yêu cầu SRS:** 3.2, 3.3 (Cancel/Release logic)
- Không có endpoint hủy order
- Không có logic hoàn cọc

---

#### 19. ❌ **Approve/Reject Phát Sinh Chi Phí**
**Yêu cầu SRS:** Staff FR - Yêu cầu phát sinh, Customer phản hồi
- Không thấy BookingExtraItem logic (entity tồn tại)
- Không có workflow duyệt phát sinh

---

#### 20. ❌ **Search & Filter**
**Yêu cầu SRS:** Tìm kiếm sản phẩm, lọc theo danh mục
- ⚠️ ProductController có lẽ có, cần verify

---

---

## BẢNG TÓM TẮT ĐO LƯỜNG TUÂN THỦ

| Nhóm Chức Năng | Yêu Cầu | Triển Khai | ✓/✗/⚠ | Độ ưu tiên |
|---|---|---|---|---|
| **Authentication** | RBAC, Ownership | 90% | ⚠️ | HIGH |
| **Product Catalog** | CRUD, Display | 100% | ✅ | HIGH |
| **Service Catalog** | CRUD, Display | 100% | ✅ | HIGH |
| **Cart Management** | Add/Remove/Update | 100% | ✅ | HIGH |
| **Fulfillment Type** | SHIPPING/AT_WORKSHOP | 0% | ❌ | **CRITICAL** |
| **Order Splitting** | Parent/Sub-order | 20% | ❌ | **CRITICAL** |
| **Slot Scheduling** | Dynamic slots | 0% | ❌ | **CRITICAL** |
| **Inventory Reservation** | Reserve/Confirm/Release | 30% | ❌ | **CRITICAL** |
| **Deposit Calculation** | 20% cọc | 30% | ⚠️ | HIGH |
| **PayOS Integration** | Create/Webhook | 10% | ❌ | **CRITICAL** |
| **State Machines** | Order/Booking states | 50% | ⚠️ | HIGH |
| **Concurrency Control** | Optimistic Lock | 50% | ⚠️ | MEDIUM |
| **Notifications** | Order/Booking alerts | 0% | ❌ | MEDIUM |
| **BI Dashboard** | Reports & Analytics | 0% | ❌ | LOW |

---

## PRIORITIZED ACTION PLAN

### 🔴 CRITICAL (Phải làm ngay)

1. **Thêm fulfillmentType vào CartItem** - 2h
   - Modify CartItem entity
   - Update CartController/Service
   - Update checkout logic

2. **Implement Order Splitting Logic** - 4h
   - Analyze cart items by fulfillmentType
   - Create parent + sub-orders
   - Refactor OrderServiceImpl.checkout()

3. **Implement Slot Scheduling Engine** - 6h
   - SchedulingService with dynamic slot calculation
   - Implement overlap detection
   - Add working_hours configuration

4. **PayOS Integration** - 4h
   - PayOS API client
   - Payment link creation
   - Webhook endpoint + signature verification

5. **Inventory Reservation Workflow** - 4h
   - Implement InventoryReservationService
   - Reserve on PENDING_DEPOSIT
   - Release on timeout/cancel
   - Confirm on PAID webhook

### 🟠 HIGH (Tuần này)

6. **Audit ID Spoofing Prevention** - 2h
   - Add @PreAuthorize to all endpoints
   - Validate ownership in Service layer

7. **State Machine Enforcement** - 3h
   - Implement valid state transitions
   - Add validators

8. **Deposit Calculation Complete** - 2h
   - Compute cọc for all scenarios
   - Implement mixed-order cọc

---

## KẾT LUẬN

**Độ hoàn thành hiện tại: ~60%**

Hệ thống đã xây dựng tốt cơ sở hạ tầng (entities, repositories, services). Tuy nhiên, **các luồng nghiệp vụ chính (fulfillment type, order splitting, slot scheduling, payment integration) còn thiếu hoặc chưa hoàn thiện.**

**Để đạt yêu cầu cao nhất, cần ưu tiên:**
1. Fulfillment Type & Order Splitting
2. PayOS Integration + Webhook
3. Slot Scheduling
4. Inventory Reservation System

**Dự kiến:** ~20-25 giờ công để hoàn thiện theo SRS.

---

*Báo cáo này được tạo tự động - Kiểm tra chi tiết tại: `COMPLIANCE_REPORT.md`*

