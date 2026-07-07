# GearShift Pro CarShowRoom & O2O Service Platform — Agent-Readable SRS

> **Mục đích tài liệu:** Mô tả nghiệp vụ, luồng xử lý, dữ liệu, trạng thái và quy tắc kỹ thuật cho AI Agent / coding agent đọc để thiết kế hoặc sinh code cho hệ thống.  
> **Ngôn ngữ phát triển dự kiến:** Java Spring Boot MVC, Thymeleaf, SQL Server, Spring Security, JPA/Hibernate.  
> **Mô hình vận hành:** O2O — Online-to-Offline E-Commerce kết hợp đặt lịch dịch vụ tại xưởng.

---

## 0. Tóm tắt hệ thống

GearShift Pro là nền tảng bán phụ tùng ô tô kết hợp đặt lịch bảo dưỡng/sửa chữa tại showroom/xưởng. Người dùng có thể:

1. Mua phụ tùng để giao hàng tận nơi.
2. Mua phụ tùng và chọn lắp đặt tại xưởng.
3. Đặt lịch dịch vụ bảo dưỡng/sửa chữa độc lập.
4. Thanh toán tiền cọc 20% qua PayOS Sandbox trước khi đơn hàng/lịch hẹn được xác nhận.
5. Theo dõi trạng thái đơn hàng, lịch hẹn, tiến độ sửa chữa và chi phí phát sinh.

Hệ thống cần đảm bảo:

- Phân quyền rõ ràng giữa Customer, Staff và Admin.
- Không cho người dùng thao tác dữ liệu không thuộc quyền sở hữu.
- Tách đơn tự động nếu giỏ hàng có cả phụ tùng giao hàng và phụ tùng lắp tại xưởng.
- Tính slot đặt lịch động theo tổng thời lượng dịch vụ.
- Giữ kho và giữ slot an toàn để tránh overselling/overbooking.
- Webhook PayOS phải xử lý bảo mật, idempotent và không cập nhật trùng.

---

## 1. Actors và quyền hạn

### 1.1. Customer

Customer là khách hàng sử dụng website.

Customer có quyền:

- Đăng ký, đăng nhập, đăng xuất.
- Xem danh mục phụ tùng.
- Xem danh mục dịch vụ.
- Thêm phụ tùng vào giỏ hàng.
- Với mỗi phụ tùng trong giỏ, bắt buộc chọn một trong hai fulfillment type:
  - `SHIPPING`: giao hàng tận nơi.
  - `AT_WORKSHOP`: giữ phụ tùng tại xưởng để lắp đặt.
- Quản lý giỏ hàng.
- Quản lý hồ sơ xe cá nhân.
- Tạo đơn hàng phụ tùng.
- Tạo lịch hẹn dịch vụ.
- Thanh toán tiền cọc qua PayOS Sandbox.
- Theo dõi đơn hàng/lịch hẹn của chính mình.
- Xem thông báo nội bộ trên web.
- Phản hồi đồng ý/từ chối chi phí phát sinh trong quá trình sửa chữa.

Customer không được:

- Truy cập dashboard Staff/Admin.
- Xem, sửa, hủy đơn hàng/lịch hẹn của khách hàng khác.
- Tự ý thay đổi trạng thái thanh toán, trạng thái sửa chữa hoặc trạng thái kho.

### 1.2. Staff / Workshop Operator

Staff là nhân viên vận hành bán hàng/xưởng.

Staff có quyền:

- Xem danh sách đơn hàng và lịch hẹn được phân công hoặc trong phạm vi vận hành.
- Tiếp nhận đơn hàng đã đặt cọc.
- Cập nhật trạng thái xử lý đơn hàng.
- Cập nhật trạng thái lịch hẹn.
- Ghi nhận xe đã đến xưởng.
- Ghi nhận bắt đầu sửa chữa.
- Thêm dịch vụ/phụ tùng phát sinh sau khi kiểm tra thực tế.
- Gửi yêu cầu duyệt phát sinh cho Customer.
- Hoàn tất sửa chữa và xuất hóa đơn cuối cùng.

Staff không được:

- Thay đổi cấu hình hệ thống cốt lõi như định mức kho, giá dịch vụ, số cầu nâng nếu không có quyền Admin.
- Sửa trực tiếp dữ liệu thanh toán PayOS.
- Bỏ qua bước duyệt phát sinh của Customer nếu phát sinh chi phí mới.

### 1.3. Admin

Admin là quản trị viên hệ thống.

Admin có quyền:

- CRUD dữ liệu nền:
  - Products.
  - Services.
  - Product-Service mapping.
  - Slots / working hours / workshop capacity.
  - Users / roles.
  - Vehicle brands/models nếu có.
- Cấu hình:
  - Thời lượng dịch vụ.
  - Khoảng giá dịch vụ.
  - Định mức tồn kho.
  - Số lượng cầu nâng/công suất xưởng.
  - Chính sách timeout thanh toán.
- Truy cập Business Intelligence Dashboard.
- Xem báo cáo:
  - Doanh thu cọc.
  - Dự báo dòng tiền còn lại.
  - Tỷ lệ lấp đầy xưởng.
  - Tồn kho đang giữ chỗ.
  - Đơn hàng/lịch hẹn bị hủy/quá hạn.

---

## 2. Các khái niệm nghiệp vụ chính

### 2.1. Fulfillment Type

Mỗi item phụ tùng trong giỏ hàng phải có một kiểu xử lý:

| Code | Ý nghĩa |
|---|---|
| `SHIPPING` | Phụ tùng được giao tận nơi cho khách. |
| `AT_WORKSHOP` | Phụ tùng được giữ tại xưởng để lắp đặt theo lịch hẹn. |

Không cho phép cart item không có fulfillment type.

### 2.2. Parent Order và Sub-Order

Nếu giỏ hàng chỉ có `SHIPPING`, hệ thống tạo một order loại `SHIPPING`.

Nếu giỏ hàng chỉ có `AT_WORKSHOP`, hệ thống tạo một order loại `AT_WORKSHOP` và bắt buộc liên kết với booking.

Nếu giỏ hàng có cả hai loại, hệ thống tạo:

- Một `Parent Order` để gom giao dịch tổng.
- Một `Sub-Order SHIPPING` cho hàng giao tận nơi.
- Một `Sub-Order AT_WORKSHOP` cho hàng giữ lại xưởng.
- Một `Booking` tương ứng cho phần lắp đặt tại xưởng.

> Khuyến nghị kỹ thuật: `Parent Order` không nên tham gia xử lý kho trực tiếp. Kho chỉ xử lý theo các sub-order cụ thể.

### 2.3. Available Stock

Trong database chỉ cần lưu:

- `physical_stock`: tổng số lượng vật lý đang có trong kho.
- `reserved_stock`: số lượng đang bị giữ chỗ cho đơn/lịch đã tạo.

`available_stock` nên là giá trị tính toán:

```text
available_stock = physical_stock - reserved_stock
```

Không nên lưu `available_stock` như một cột riêng nếu không có lý do đặc biệt, vì dễ lệch dữ liệu khi cập nhật đồng thời.

### 2.4. Tiền cọc

Mọi đơn hàng/lịch hẹn cần xác nhận đều yêu cầu đặt cọc 20%.

Quy tắc:

```text
deposit_amount = deposit_base_amount * 20%
```

Trong đó:

- Với đơn hàng phụ tùng: `deposit_base_amount = sum(product_price * quantity)`.
- Với lịch hẹn dịch vụ: `deposit_base_amount = sum(service.min_price)`.
- Với luồng O2O tích hợp: `deposit_base_amount = product_total + service_min_total`.

Cần làm tròn tiền theo đơn vị VND. Khuyến nghị dùng `BigDecimal` trong Java, không dùng `double`.

---

## 3. Điểm logic cần chỉnh / chuẩn hóa trước khi code

Phần này là kết luận rà soát logic từ SRS gốc. Coding agent phải ưu tiên các quy tắc đã chuẩn hóa dưới đây.

### 3.1. Kho không nên đợi đến lúc thanh toán xong mới giữ

SRS gốc ghi:

- `Pending Deposit`: chưa tác động kho.
- `Deposited`: mới giảm available và tăng reserved.

Rủi ro: nếu chỉ giữ kho sau khi thanh toán, hai khách có thể cùng tạo link cọc cho món phụ tùng cuối cùng. Khi cả hai thanh toán thành công, hệ thống bị overselling.

Logic chuẩn hóa nên dùng:

- Khi tạo payment link thành công và chuyển sang `PENDING_DEPOSIT`, hệ thống giữ tạm kho trong 15 phút.
- Nếu khách thanh toán thành công: giữ chỗ chuyển từ tạm sang chính thức.
- Nếu quá hạn/hủy/chưa thanh toán: giải phóng kho.

Nên xem `RESERVED_STOCK` là cả giữ tạm và giữ chính thức, nhưng cần bảng chi tiết reservation để biết reservation nào hết hạn.

### 3.2. Slot đặt lịch cũng phải được giữ tạm trong lúc chờ cọc

Nếu không giữ slot trong `PENDING_DEPOSIT`, nhiều khách có thể cùng thanh toán cho một slot.

Logic chuẩn hóa:

- Khi booking được tạo và payment link thành công: slot bị giữ tạm trong 15 phút.
- Booking ở `PENDING_DEPOSIT` vẫn được tính là chiếm capacity cho đến khi hết hạn.
- Nếu thanh toán thành công: booking thành `CONFIRMED`.
- Nếu quá hạn 15 phút: booking thành `CANCELED` hoặc `EXPIRED_PAYMENT`, slot được giải phóng.

### 3.3. Completed không được “giải phóng reserved về available”

Khi đơn hoàn tất, hàng đã thực sự rời kho. Vì vậy:

```text
on reserve:
  physical_stock unchanged
  reserved_stock += quantity

on complete:
  physical_stock -= quantity
  reserved_stock -= quantity

on cancel before complete:
  physical_stock unchanged
  reserved_stock -= quantity
```

Không được cộng ngược vào available khi đơn đã hoàn thành.

### 3.4. Booking cần tách estimated amount và final amount

Dịch vụ sửa chữa có khoảng giá `min_price` - `max_price`, nên `total_amount` ban đầu chưa phải số cuối cùng.

Nên dùng:

- `estimated_min_amount`
- `estimated_max_amount`
- `deposit_amount`
- `final_amount`
- `remaining_amount`

Trong đó:

```text
deposit_amount = estimated_min_amount * 20%
remaining_amount = final_amount - deposit_amount
```

`final_amount` chỉ có sau khi sửa chữa hoàn tất hoặc sau khi khách duyệt phát sinh.

### 3.5. PayOS nên có bảng Payment Transaction riêng

Không nên chỉ lưu `payos_order_code` trong `Orders` và `Bookings`, vì một giao dịch PayOS có thể liên quan đến nhiều entity trong luồng O2O.

Khuyến nghị thêm bảng `Payment_Transactions`:

- Giao dịch cọc cho order thuần.
- Giao dịch cọc cho booking thuần.
- Giao dịch cọc tổng cho luồng tích hợp gồm parent order + sub-orders + booking.

Webhook PayOS cập nhật `Payment_Transactions` trước, sau đó gọi domain service để chuyển trạng thái order/booking tương ứng.

---

## 4. Functional Requirements

### 4.1. Authentication & RBAC

#### FR-AUTH-001 — Đăng ký tài khoản

Customer có thể đăng ký bằng email, mật khẩu và thông tin cơ bản.

Validation tối thiểu:

- Email đúng định dạng và unique.
- Password được hash bằng BCrypt.
- Role mặc định là `CUSTOMER`.
- Không lưu plain text password.

#### FR-AUTH-002 — Đăng nhập

Người dùng đăng nhập bằng email và password.

Sau khi đăng nhập:

- `CUSTOMER` vào trang khách hàng.
- `STAFF` vào dashboard vận hành.
- `ADMIN` vào dashboard quản trị.

#### FR-AUTH-003 — Phân quyền URL và method

Sử dụng Spring Security:

- URL `/admin/**` chỉ cho `ADMIN`.
- URL `/staff/**` cho `STAFF` hoặc `ADMIN`.
- URL `/customer/**` cho `CUSTOMER`.
- Các action nhạy cảm phải có `@PreAuthorize` ở Controller/Service.

Ví dụ:

```java
@PreAuthorize("hasRole('ADMIN')")
public void updateServicePrice(...)
```

#### FR-AUTH-004 — Chống ID Spoofing

Mọi thao tác theo ID phải kiểm tra ownership hoặc quyền role.

Ví dụ:

- Customer chỉ được xem booking nếu `booking.user.id == currentUser.id`.
- Customer chỉ được hủy order nếu order thuộc user đó và trạng thái cho phép hủy.
- Staff/Admin được xem theo quyền vận hành.

---

### 4.2. Product Catalog

#### FR-PROD-001 — Xem danh sách phụ tùng

Customer có thể xem danh sách sản phẩm đang active.

Thông tin hiển thị:

- Tên phụ tùng.
- SKU.
- Giá.
- Ảnh.
- Mô tả ngắn.
- Số lượng còn có thể bán: `physical_stock - reserved_stock`.
- Nút thêm vào giỏ.

#### FR-PROD-002 — Admin quản lý phụ tùng

Admin có thể CRUD product.

Các trường chính:

- `name`
- `sku`
- `price`
- `physical_stock`
- `reserved_stock`
- `image`
- `status`
- `version`

Ràng buộc:

- `sku` unique.
- `price >= 0`.
- `physical_stock >= 0`.
- `reserved_stock >= 0`.
- `reserved_stock <= physical_stock`.

---

### 4.3. Service Catalog

#### FR-SERV-001 — Xem danh sách dịch vụ

Customer có thể xem các dịch vụ đang active.

Thông tin hiển thị:

- Tên dịch vụ.
- Thời lượng ước tính.
- Giá tối thiểu.
- Giá tối đa.
- Mô tả.

#### FR-SERV-002 — Admin quản lý dịch vụ

Admin có thể CRUD service.

Các trường chính:

- `name`
- `duration_minutes`
- `min_price`
- `max_price`
- `description`
- `status`

Ràng buộc:

- `duration_minutes > 0`.
- `min_price >= 0`.
- `max_price >= min_price`.

---

### 4.4. Vehicle Profile

#### FR-VEH-001 — Customer quản lý xe cá nhân

Customer có thể thêm/sửa/xóa hồ sơ xe của chính mình.

Thông tin xe:

- Biển số.
- Hãng xe.
- Dòng xe/đời xe.
- Năm sản xuất nếu có.
- Số km hiện tại.

Ràng buộc:

- Biển số không được rỗng.
- Số km hiện tại `>= 0`.
- Customer không được sửa xe của người khác.

---

### 4.5. Smart Cart

#### FR-CART-001 — Thêm phụ tùng vào giỏ

Khi thêm product vào cart, Customer phải chọn fulfillment type:

- `SHIPPING`
- `AT_WORKSHOP`

Cart item gồm:

- `product_id`
- `quantity`
- `fulfillment_type`

Validation:

- Product phải active.
- Quantity > 0.
- Quantity không được vượt quá available stock tại thời điểm thêm hoặc checkout.

#### FR-CART-002 — Lưu giỏ hàng

Giỏ hàng có thể lưu trong session và đồng bộ database nếu user đã đăng nhập.

Yêu cầu:

- F5 không làm mất giỏ hàng.
- Sau đăng nhập, nếu session cart có dữ liệu thì merge vào database cart.
- Nếu trùng product + fulfillment type thì cộng số lượng.

#### FR-CART-003 — Phân loại cart khi checkout

Khi checkout:

- Nhóm item `SHIPPING` vào shipping group.
- Nhóm item `AT_WORKSHOP` vào workshop group.

Nếu có workshop group:

- Bắt buộc Customer chọn xe.
- Bắt buộc Customer chọn dịch vụ tương ứng.
- Bắt buộc Customer chọn slot động đủ thời gian.

---

### 4.6. Order Splitting

#### FR-ORDER-001 — Checkout SHIPPING only

Nếu cart chỉ có item `SHIPPING`:

1. Validate stock.
2. Tạo order loại `SHIPPING`.
3. Tính tổng tiền sản phẩm.
4. Tính cọc 20%.
5. Tạo payment transaction.
6. Tạo PayOS checkout URL.
7. Reserve stock tạm trong 15 phút.
8. Chuyển order sang `PENDING_DEPOSIT`.
9. Redirect Customer sang PayOS.

#### FR-ORDER-002 — Checkout AT_WORKSHOP only

Nếu cart chỉ có item `AT_WORKSHOP`:

1. Customer chọn xe.
2. Customer chọn dịch vụ.
3. Hệ thống tính tổng thời lượng dịch vụ.
4. Hệ thống hiển thị slot còn trống.
5. Customer chọn slot.
6. Validate stock và capacity.
7. Tạo order loại `AT_WORKSHOP`.
8. Tạo booking.
9. Tính cọc sản phẩm + cọc dịch vụ.
10. Tạo payment transaction.
11. Tạo PayOS checkout URL.
12. Reserve stock và giữ slot tạm trong 15 phút.
13. Chuyển order và booking sang `PENDING_DEPOSIT`.
14. Redirect Customer sang PayOS.

#### FR-ORDER-003 — Checkout mixed cart

Nếu cart có cả `SHIPPING` và `AT_WORKSHOP`:

1. Tạo parent order.
2. Tạo sub-order `SHIPPING`.
3. Tạo sub-order `AT_WORKSHOP`.
4. Tạo booking cho phần workshop.
5. Tính tổng cọc:

```text
total_deposit = shipping_product_deposit + workshop_product_deposit + service_deposit
```

6. Tạo một payment transaction cho toàn bộ tiền cọc.
7. Reserve stock cho cả hai sub-order.
8. Giữ slot booking tạm trong 15 phút.
9. Chuyển parent order, sub-orders và booking sang `PENDING_DEPOSIT`.
10. Redirect Customer sang PayOS.

---

### 4.7. Dynamic Slot Scheduling Engine

#### FR-SLOT-001 — Tính tổng thời lượng dịch vụ

Khi Customer chọn nhiều service:

```text
total_duration_minutes = sum(service.duration_minutes)
```

Ví dụ:

- Thay nhớt: 30 phút.
- Kiểm tra phanh: 60 phút.
- Tổng thời lượng: 90 phút.

#### FR-SLOT-002 — Sinh slot động

Không dùng slot cố định đơn giản như sáng/chiều. Hệ thống phải quét lịch làm việc của xưởng và hiển thị khoảng trống liên tục đủ dài cho `total_duration_minutes`.

Input:

- Ngày booking.
- Tổng thời lượng dịch vụ.
- Working hours.
- Số cầu nâng/capacity.
- Danh sách booking đang chiếm capacity.

Booking được tính là chiếm capacity nếu có status:

- `PENDING_DEPOSIT` và chưa quá hạn thanh toán.
- `CONFIRMED`.
- `IN_PROGRESS`.
- `PENDING_APPROVAL`.

Booking không chiếm capacity nếu có status:

- `CANCELED`.
- `EXPIRED_PAYMENT`.
- `EXPIRED_NO_SHOW`.
- `COMPLETED`.

Khuyến nghị thuật toán:

1. Chia ngày làm việc thành các mốc nhỏ, ví dụ mỗi 15 phút.
2. Với mỗi candidate start time:
   - candidate end time = start + total duration.
   - Nếu candidate nằm ngoài giờ làm việc thì bỏ qua.
   - Kiểm tra mọi đoạn 15 phút trong khoảng start-end.
   - Tại mỗi đoạn, đếm số booking overlap.
   - Nếu số booking overlap < capacity cho toàn bộ khoảng start-end thì slot hợp lệ.
3. Trả về danh sách slot hợp lệ.

#### FR-SLOT-003 — Quy tắc overlap

Hai booking được xem là overlap nếu:

```text
existing.start_time < candidate.end_time
AND existing.end_time > candidate.start_time
```

#### FR-SLOT-004 — Giữ slot khi checkout

Khi Customer chọn slot và tạo payment link:

- Booking chuyển sang `PENDING_DEPOSIT`.
- `payment_deadline` = thời điểm hiện tại + 15 phút.
- Slot đó bị tính là chiếm capacity tạm thời.

Nếu quá deadline mà chưa thanh toán:

- Booking chuyển sang `EXPIRED_PAYMENT`.
- Slot được giải phóng.

---

### 4.8. Inventory Locking & Reservation

#### FR-INV-001 — Kiểm tra available stock

Trước khi tạo order/payment:

```text
available_stock = physical_stock - reserved_stock
```

Nếu `available_stock < requested_quantity`, không cho checkout.

#### FR-INV-002 — Reserve stock

Khi order/booking chuyển sang `PENDING_DEPOSIT`:

```text
reserved_stock += quantity
```

Đồng thời tạo bản ghi reservation chi tiết.

Reservation cần có:

- `product_id`
- `order_id` hoặc `booking_id`
- `quantity`
- `reservation_status`
- `expires_at`

#### FR-INV-003 — Confirm reservation

Khi PayOS webhook xác nhận thanh toán thành công:

- Payment transaction chuyển `PAID`.
- Order/Booking chuyển sang trạng thái đã cọc/xác nhận.
- Reservation chuyển `CONFIRMED`.
- `reserved_stock` giữ nguyên.

#### FR-INV-004 — Release reservation

Khi hủy hoặc hết hạn trước khi hoàn thành:

```text
reserved_stock -= quantity
```

Reservation chuyển `RELEASED`.

#### FR-INV-005 — Commit reservation khi hoàn tất

Khi đơn hàng hoàn tất hoặc phụ tùng đã được dùng thật:

```text
physical_stock -= quantity
reserved_stock -= quantity
```

Reservation chuyển `CONSUMED`.

#### FR-INV-006 — Concurrency control

Dùng Optimistic Locking bằng JPA `@Version` trên `Product`.

Nếu update stock bị conflict:

- Retry có giới hạn hoặc trả lỗi “Sản phẩm vừa được người khác đặt, vui lòng thử lại”.
- Không được tạo payment link nếu reserve stock thất bại.

---

### 4.9. Deposit Calculation

#### FR-PAY-001 — Cọc đơn phụ tùng

```text
product_total = sum(product.price * quantity)
deposit_amount = product_total * 0.2
```

#### FR-PAY-002 — Cọc lịch hẹn dịch vụ

```text
service_min_total = sum(service.min_price)
deposit_amount = service_min_total * 0.2
```

#### FR-PAY-003 — Cọc O2O tích hợp

```text
shipping_product_total = sum(shipping product.price * quantity)
workshop_product_total = sum(workshop product.price * quantity)
service_min_total = sum(service.min_price)

deposit_amount = (shipping_product_total + workshop_product_total + service_min_total) * 0.2
```

#### FR-PAY-004 — Số tiền còn lại

Với order phụ tùng:

```text
remaining_amount = product_total - deposit_amount
```

Với booking:

```text
remaining_amount = final_amount - deposit_amount
```

Với booking chưa có final amount:

```text
remaining_amount chỉ là dự kiến, không phải công nợ cuối cùng
```

---

### 4.10. PayOS Integration Workflow

#### FR-PAYOS-001 — Create payment link

Sau khi validate order/booking và reserve tạm thành công:

1. Tạo `PaymentTransaction` status `INITIATED`.
2. Gọi PayOS API tạo checkout link với số tiền cọc.
3. Lưu:
   - `payos_order_code`
   - `checkout_url`
   - `payment_deadline`
4. Chuyển PaymentTransaction sang `PENDING`.
5. Chuyển order/booking sang `PENDING_DEPOSIT`.
6. Redirect Customer sang `checkout_url`.

#### FR-PAYOS-002 — Return URL

Return URL chỉ dùng để hiển thị UI cho Customer.

Không tin return URL là bằng chứng thanh toán.

Các route gợi ý:

- `/checkout/success`
- `/checkout/cancel`

Trang success nên hiển thị:

- “Thanh toán đang được xác nhận. Vui lòng kiểm tra trạng thái đơn hàng/lịch hẹn.”

Trạng thái thật phải dựa trên webhook.

#### FR-PAYOS-003 — Webhook

Endpoint backend:

```text
POST /api/payos/webhook
```

Yêu cầu:

- Không cần login session.
- Phải xác minh checksum/signature từ PayOS.
- Phải idempotent.
- Nếu webhook trùng, không được cộng/trừ kho nhiều lần.
- Cập nhật PaymentTransaction trước.
- Gọi domain service để chuyển trạng thái order/booking.

Pseudo flow:

```text
receive webhook
verify checksum
find payment_transaction by payos_order_code
if transaction already PAID:
  return OK
if webhook status is PAID:
  mark transaction PAID
  mark related order/booking deposited/confirmed
  confirm reservations
else if webhook status is CANCELED/FAILED:
  mark transaction FAILED/CANCELED
  cancel related order/booking if allowed
  release reservations
return OK
```

---

## 5. State Machines

### 5.1. Order Status

Enum đề xuất:

```text
CREATED
PENDING_DEPOSIT
DEPOSITED
PROCESSING
SHIPPING
COMPLETED
CANCELED
EXPIRED_PAYMENT
```

#### 5.1.1. Order transition table

| From | To | Trigger | Side effects |
|---|---|---|---|
| `CREATED` | `PENDING_DEPOSIT` | PayOS link created | Reserve stock tạm, set payment deadline. |
| `PENDING_DEPOSIT` | `DEPOSITED` | PayOS webhook paid | Confirm reservation. |
| `PENDING_DEPOSIT` | `EXPIRED_PAYMENT` | Quá 15 phút chưa thanh toán | Release stock. |
| `PENDING_DEPOSIT` | `CANCELED` | Customer hủy trước khi thanh toán | Release stock. |
| `DEPOSITED` | `PROCESSING` | Staff tiếp nhận | Stock vẫn reserved. |
| `PROCESSING` | `SHIPPING` | Staff bàn giao vận chuyển | Stock vẫn reserved. |
| `SHIPPING` | `COMPLETED` | Khách nhận hàng và thanh toán phần còn lại | physical_stock -= qty, reserved_stock -= qty. |
| `DEPOSITED`/`PROCESSING` | `CANCELED` | Hủy hợp lệ trước khi giao | Release stock theo chính sách hoàn cọc nếu có. |

#### 5.1.2. Quy tắc hủy order

Customer chỉ được hủy khi order chưa vào `SHIPPING` hoặc `COMPLETED`.

Staff/Admin có thể hủy trong các trạng thái vận hành, nhưng phải ghi lý do.

Nếu order đã `COMPLETED`, không chuyển về canceled.

---

### 5.2. Booking Status

Enum đề xuất:

```text
CREATED
PENDING_DEPOSIT
CONFIRMED
IN_PROGRESS
PENDING_APPROVAL
COMPLETED
CANCELED
EXPIRED_PAYMENT
EXPIRED_NO_SHOW
```

#### 5.2.1. Booking transition table

| From | To | Trigger | Side effects |
|---|---|---|---|
| `CREATED` | `PENDING_DEPOSIT` | PayOS link created | Hold slot + reserve workshop parts tạm. |
| `PENDING_DEPOSIT` | `CONFIRMED` | PayOS webhook paid | Confirm slot + reservation. |
| `PENDING_DEPOSIT` | `EXPIRED_PAYMENT` | Quá 15 phút chưa thanh toán | Release slot + release stock. |
| `PENDING_DEPOSIT` | `CANCELED` | Customer hủy trước thanh toán | Release slot + release stock. |
| `CONFIRMED` | `IN_PROGRESS` | Khách đến và Staff bắt đầu sửa | Xe đang sửa. |
| `CONFIRMED` | `EXPIRED_NO_SHOW` | Quá 30 phút so với giờ hẹn mà khách không đến | Release slot + release stock theo chính sách. |
| `IN_PROGRESS` | `PENDING_APPROVAL` | Staff thêm phát sinh cần khách duyệt | Chưa được tính vào final bill nếu khách chưa đồng ý. |
| `PENDING_APPROVAL` | `IN_PROGRESS` | Customer đồng ý/từ chối phát sinh | Cập nhật work order. |
| `IN_PROGRESS` | `COMPLETED` | Sửa xong, nghiệm thu | Tính final amount, consume used parts, xuất hóa đơn. |

#### 5.2.2. Quy tắc no-show

Nếu quá 30 phút so với `booking_start_time` mà booking vẫn `CONFIRMED` và Staff đánh dấu khách không đến hoặc job scheduler tự kiểm tra:

- Booking chuyển `EXPIRED_NO_SHOW`.
- Slot được giải phóng.
- Phụ tùng giữ chỗ được release nếu chưa dùng.
- Tiền cọc xử lý theo chính sách nội bộ. Trong phạm vi đồ án có thể chỉ ghi nhận là `deposit_forfeited` hoặc `deposit_status = NON_REFUNDABLE` nếu muốn đơn giản.

---

### 5.3. Payment Status

Enum đề xuất:

```text
INITIATED
PENDING
PAID
FAILED
CANCELED
EXPIRED
REFUNDED
```

`PAID` chỉ được set từ webhook hợp lệ hoặc admin reconciliation có log đầy đủ. Không được set từ return URL.

---

### 5.4. Reservation Status

Enum đề xuất:

```text
HELD
CONFIRMED
RELEASED
CONSUMED
EXPIRED
```

Ý nghĩa:

- `HELD`: giữ tạm trong lúc chờ cọc.
- `CONFIRMED`: đã thanh toán cọc, vẫn đang giữ.
- `RELEASED`: đã trả lại kho.
- `CONSUMED`: đã dùng thật / đã bán thật.
- `EXPIRED`: hết hạn giữ tạm, thường đi kèm release stock.

---

## 6. Database Schema đề xuất

### 6.1. users

```text
id BIGINT PK
email VARCHAR UNIQUE NOT NULL
password_hash VARCHAR NOT NULL
full_name NVARCHAR
phone VARCHAR
role VARCHAR NOT NULL -- CUSTOMER/STAFF/ADMIN
status VARCHAR NOT NULL -- ACTIVE/LOCKED/DISABLED
created_at DATETIME2
updated_at DATETIME2
```

### 6.2. vehicles

```text
id BIGINT PK
user_id BIGINT FK users(id)
license_plate VARCHAR NOT NULL
brand NVARCHAR NOT NULL
model NVARCHAR NOT NULL
manufacture_year INT NULL
current_km INT NOT NULL
status VARCHAR NOT NULL
created_at DATETIME2
updated_at DATETIME2
```

### 6.3. products

```text
id BIGINT PK
name NVARCHAR NOT NULL
sku VARCHAR UNIQUE NOT NULL
price DECIMAL(18,2) NOT NULL
physical_stock INT NOT NULL
reserved_stock INT NOT NULL DEFAULT 0
image_url VARCHAR NULL
description NVARCHAR(MAX) NULL
status VARCHAR NOT NULL -- ACTIVE/INACTIVE
version BIGINT NOT NULL
created_at DATETIME2
updated_at DATETIME2
```

Computed rule:

```text
available_stock = physical_stock - reserved_stock
```

### 6.4. services

```text
id BIGINT PK
name NVARCHAR NOT NULL
duration_minutes INT NOT NULL
min_price DECIMAL(18,2) NOT NULL
max_price DECIMAL(18,2) NOT NULL
description NVARCHAR(MAX) NULL
status VARCHAR NOT NULL -- ACTIVE/INACTIVE
created_at DATETIME2
updated_at DATETIME2
```

### 6.5. product_service

```text
product_id BIGINT FK products(id)
service_id BIGINT FK services(id)
PRIMARY KEY(product_id, service_id)
```

Ý nghĩa: định nghĩa phụ tùng nào thường đi kèm dịch vụ nào.

### 6.6. orders

```text
id BIGINT PK
parent_order_id BIGINT NULL FK orders(id)
user_id BIGINT FK users(id)
order_type VARCHAR NOT NULL -- PARENT/SHIPPING/AT_WORKSHOP
order_status VARCHAR NOT NULL
payment_status VARCHAR NOT NULL
product_total DECIMAL(18,2) NOT NULL DEFAULT 0
deposit_amount DECIMAL(18,2) NOT NULL DEFAULT 0
remaining_amount DECIMAL(18,2) NOT NULL DEFAULT 0
payos_order_code VARCHAR NULL
created_at DATETIME2
updated_at DATETIME2
```

### 6.7. order_items

```text
id BIGINT PK
order_id BIGINT FK orders(id)
product_id BIGINT FK products(id)
quantity INT NOT NULL
unit_price DECIMAL(18,2) NOT NULL
line_total DECIMAL(18,2) NOT NULL
fulfillment_type VARCHAR NOT NULL -- SHIPPING/AT_WORKSHOP
created_at DATETIME2
```

### 6.8. bookings

```text
id BIGINT PK
user_id BIGINT FK users(id)
vehicle_id BIGINT FK vehicles(id)
related_order_id BIGINT NULL FK orders(id)
booking_date DATE NOT NULL
start_time TIME NOT NULL
end_time TIME NOT NULL
total_duration_minutes INT NOT NULL
estimated_min_amount DECIMAL(18,2) NOT NULL
estimated_max_amount DECIMAL(18,2) NOT NULL
deposit_amount DECIMAL(18,2) NOT NULL
final_amount DECIMAL(18,2) NULL
remaining_amount DECIMAL(18,2) NULL
booking_status VARCHAR NOT NULL
payment_status VARCHAR NOT NULL
payos_order_code VARCHAR NULL
payment_deadline DATETIME2 NULL
created_at DATETIME2
updated_at DATETIME2
```

### 6.9. booking_services

```text
id BIGINT PK
booking_id BIGINT FK bookings(id)
service_id BIGINT FK services(id)
service_name_snapshot NVARCHAR NOT NULL
duration_minutes_snapshot INT NOT NULL
min_price_snapshot DECIMAL(18,2) NOT NULL
max_price_snapshot DECIMAL(18,2) NOT NULL
created_at DATETIME2
```

Dùng snapshot để nếu Admin đổi giá/duration sau này, booking cũ không bị thay đổi sai.

### 6.10. payment_transactions

```text
id BIGINT PK
user_id BIGINT FK users(id)
parent_order_id BIGINT NULL FK orders(id)
order_id BIGINT NULL FK orders(id)
booking_id BIGINT NULL FK bookings(id)
payos_order_code VARCHAR UNIQUE NOT NULL
amount DECIMAL(18,2) NOT NULL
status VARCHAR NOT NULL -- INITIATED/PENDING/PAID/FAILED/CANCELED/EXPIRED/REFUNDED
checkout_url VARCHAR NULL
payment_deadline DATETIME2 NOT NULL
paid_at DATETIME2 NULL
raw_webhook_payload NVARCHAR(MAX) NULL
created_at DATETIME2
updated_at DATETIME2
```

### 6.11. inventory_reservations

```text
id BIGINT PK
product_id BIGINT FK products(id)
order_id BIGINT NULL FK orders(id)
booking_id BIGINT NULL FK bookings(id)
quantity INT NOT NULL
reservation_status VARCHAR NOT NULL -- HELD/CONFIRMED/RELEASED/CONSUMED/EXPIRED
expires_at DATETIME2 NULL
created_at DATETIME2
updated_at DATETIME2
```

### 6.12. booking_extra_items

```text
id BIGINT PK
booking_id BIGINT FK bookings(id)
product_id BIGINT NULL FK products(id)
service_id BIGINT NULL FK services(id)
description NVARCHAR(MAX) NOT NULL
quantity INT NOT NULL DEFAULT 1
unit_price DECIMAL(18,2) NOT NULL
line_total DECIMAL(18,2) NOT NULL
approval_status VARCHAR NOT NULL -- PENDING/APPROVED/REJECTED
created_by_staff_id BIGINT FK users(id)
created_at DATETIME2
updated_at DATETIME2
```

### 6.13. notifications

```text
id BIGINT PK
user_id BIGINT FK users(id)
title NVARCHAR NOT NULL
message NVARCHAR(MAX) NOT NULL
notification_type VARCHAR NOT NULL
is_read BIT NOT NULL DEFAULT 0
created_at DATETIME2
```

---

## 7. Service Layer gợi ý cho coding agent

Nên chia domain service rõ ràng, không nhồi toàn bộ vào Controller.

### 7.1. CartService

Trách nhiệm:

- Add/update/remove cart item.
- Merge session cart với database cart.
- Validate cart trước checkout.
- Group cart items theo fulfillment type.

### 7.2. CheckoutService

Trách nhiệm:

- Điều phối checkout.
- Tạo order/sub-order/booking.
- Gọi InventoryReservationService.
- Gọi SchedulingService để validate slot.
- Gọi DepositCalculationService.
- Gọi PaymentService để tạo PayOS checkout link.

### 7.3. InventoryReservationService

Trách nhiệm:

- Check available stock.
- Reserve stock.
- Confirm reservation.
- Release reservation.
- Consume reservation.
- Xử lý optimistic locking.

### 7.4. SchedulingService

Trách nhiệm:

- Tính tổng duration.
- Sinh available slots.
- Validate slot còn capacity.
- Hold slot bằng booking status `PENDING_DEPOSIT`.
- Expire booking quá hạn thanh toán.

### 7.5. PaymentService

Trách nhiệm:

- Tạo PayOS payment link.
- Lưu PaymentTransaction.
- Verify webhook.
- Đảm bảo idempotency.
- Gọi OrderWorkflowService/BookingWorkflowService sau khi paid/failed.

### 7.6. OrderWorkflowService

Trách nhiệm:

- Chuyển trạng thái order hợp lệ.
- Không cho chuyển trạng thái sai thứ tự.
- Gọi inventory side effects tương ứng.

### 7.7. BookingWorkflowService

Trách nhiệm:

- Chuyển trạng thái booking hợp lệ.
- Xử lý no-show.
- Xử lý phát sinh.
- Tính final bill.

### 7.8. DashboardService

Trách nhiệm:

- Tính utilization rate.
- Tính doanh thu cọc.
- Tính forecast remaining cash.
- Tính tồn kho reserved.

---

## 8. API / Route gợi ý

### 8.1. Customer routes

```text
GET  /products
GET  /products/{id}
GET  /services
GET  /services/{id}

GET  /cart
POST /cart/items
POST /cart/items/{id}/update
POST /cart/items/{id}/remove

GET  /vehicles
POST /vehicles
POST /vehicles/{id}/update
POST /vehicles/{id}/delete

GET  /checkout
POST /checkout/confirm
GET  /checkout/success
GET  /checkout/cancel

GET  /orders
GET  /orders/{id}
POST /orders/{id}/cancel

GET  /bookings
GET  /bookings/{id}
POST /bookings/{id}/cancel
POST /bookings/{id}/extras/{extraId}/approve
POST /bookings/{id}/extras/{extraId}/reject
```

### 8.2. Staff routes

```text
GET  /staff/orders
GET  /staff/orders/{id}
POST /staff/orders/{id}/process
POST /staff/orders/{id}/ship
POST /staff/orders/{id}/complete
POST /staff/orders/{id}/cancel

GET  /staff/bookings
GET  /staff/bookings/{id}
POST /staff/bookings/{id}/start
POST /staff/bookings/{id}/add-extra
POST /staff/bookings/{id}/complete
POST /staff/bookings/{id}/mark-no-show
```

### 8.3. Admin routes

```text
GET  /admin/dashboard

GET  /admin/products
POST /admin/products
POST /admin/products/{id}/update
POST /admin/products/{id}/delete

GET  /admin/services
POST /admin/services
POST /admin/services/{id}/update
POST /admin/services/{id}/delete

GET  /admin/users
POST /admin/users/{id}/lock
POST /admin/users/{id}/unlock
```

### 8.4. Public / system routes

```text
POST /api/payos/webhook
GET  /api/booking-slots?date=yyyy-MM-dd&serviceIds=1,2,3
```

---

## 9. Core business algorithms

### 9.1. Checkout mixed cart pseudo flow

```text
function checkout(cart, selectedVehicle, selectedServices, selectedSlot):
  assert user is authenticated
  assert cart is not empty

  shippingItems = cart.items where fulfillment_type = SHIPPING
  workshopItems = cart.items where fulfillment_type = AT_WORKSHOP

  if workshopItems not empty:
    assert selectedVehicle belongs to current user
    assert selectedServices not empty
    totalDuration = sum(service.duration_minutes)
    assert selectedSlot has enough capacity for totalDuration

  begin transaction

  validate stock for all cart items

  if shippingItems and workshopItems:
    parentOrder = create order type PARENT
    shippingOrder = create order type SHIPPING with parentOrder
    workshopOrder = create order type AT_WORKSHOP with parentOrder
    booking = create booking linked to workshopOrder
  else if shippingItems only:
    order = create order type SHIPPING
  else if workshopItems only:
    order = create order type AT_WORKSHOP
    booking = create booking linked to order

  reserve stock for all order items
  hold slot if booking exists

  deposit = calculateDeposit(order(s), booking)
  paymentTransaction = create PayOS payment transaction

  set order(s) status PENDING_DEPOSIT
  set booking status PENDING_DEPOSIT if exists

  commit transaction

  redirect user to PayOS checkout_url
```

### 9.2. PayOS webhook pseudo flow

```text
function handlePayOSWebhook(payload):
  verify checksum
  orderCode = payload.orderCode

  begin transaction

  payment = find PaymentTransaction by orderCode with lock

  if payment.status == PAID:
    commit
    return OK

  if payload indicates paid:
    payment.status = PAID
    payment.paid_at = now

    if payment.parent_order_id exists:
      mark parent and sub-orders DEPOSITED
      mark related booking CONFIRMED if exists
    else:
      mark order DEPOSITED if exists
      mark booking CONFIRMED if exists

    confirm reservations
    create notification for customer

  else if payload indicates failed/canceled:
    payment.status = FAILED/CANCELED
    cancel related order/booking if currently PENDING_DEPOSIT
    release reservations
    create notification for customer

  commit
  return OK
```

### 9.3. Expire pending payment job

Chạy định kỳ, ví dụ mỗi 1 phút hoặc 5 phút.

```text
function expirePendingPayments():
  payments = find PENDING transactions where payment_deadline < now

  for each payment:
    begin transaction
    if payment.status is still PENDING:
      payment.status = EXPIRED
      mark related order/booking EXPIRED_PAYMENT if still PENDING_DEPOSIT
      release reservations
      release held slot automatically by status change
      create notification
    commit
```

### 9.4. Slot generation algorithm

```text
function getAvailableSlots(date, serviceIds):
  totalDuration = sum duration of selected services
  workingStart = configured opening time
  workingEnd = configured closing time
  step = 15 minutes
  capacity = configured lift capacity

  activeBookings = bookings on date where status in
    PENDING_DEPOSIT not expired,
    CONFIRMED,
    IN_PROGRESS,
    PENDING_APPROVAL

  result = []

  for candidateStart from workingStart to workingEnd - totalDuration step 15 minutes:
    candidateEnd = candidateStart + totalDuration
    valid = true

    for t from candidateStart to candidateEnd step 15 minutes:
      overlapCount = count activeBookings where booking.start < t + step and booking.end > t
      if overlapCount >= capacity:
        valid = false
        break

    if valid:
      result.add(candidateStart, candidateEnd)

  return result
```

---

## 10. Business Intelligence Dashboard

### 10.1. Utilization Rate

Tỷ lệ lấp đầy xưởng trong một ngày:

```text
utilization_rate = booked_minutes / total_capacity_minutes * 100
```

Trong đó:

```text
total_capacity_minutes = working_minutes_per_day * max_lift_capacity
booked_minutes = sum(duration of confirmed/in_progress/completed bookings)
```

Có thể hiển thị theo:

- Ngày.
- Tuần.
- Tháng.

### 10.2. Deposit Revenue

```text
deposit_revenue = sum(payment_transactions.amount where status = PAID)
```

### 10.3. Forecast Remaining Cash

Với order phụ tùng:

```text
forecast_remaining = sum(order.remaining_amount where order_status in DEPOSITED, PROCESSING, SHIPPING)
```

Với booking:

```text
forecast_remaining = sum(estimated_min_amount - deposit_amount for confirmed bookings not completed)
```

Nếu có `final_amount`, dùng:

```text
forecast_remaining = final_amount - deposit_amount
```

### 10.4. Reserved Inventory Report

```text
reserved_inventory = sum(inventory_reservations.quantity where status in HELD, CONFIRMED)
```

---

## 11. Non-Functional Requirements

### 11.1. Security

- Password phải hash bằng BCrypt.
- Bật CSRF protection cho form Thymeleaf.
- Phân quyền URL + method-level security.
- Kiểm tra ownership mọi tài nguyên theo user.
- Webhook PayOS phải verify checksum.
- Không log thông tin nhạy cảm như password, secret key, raw token.

### 11.2. Data Integrity

- Dùng transaction ở service layer cho checkout, payment webhook, reserve/release stock.
- Dùng optimistic locking với `@Version` trên bảng product.
- Không cập nhật stock thủ công ở nhiều nơi. Mọi thao tác kho phải đi qua `InventoryReservationService`.
- Không cập nhật status tùy tiện. Mọi chuyển trạng thái phải đi qua workflow service.

### 11.3. Reliability

- Webhook phải idempotent.
- Expire job phải an toàn khi chạy nhiều lần.
- Return URL không được quyết định trạng thái thanh toán.
- Nếu PayOS create link thất bại, rollback order/booking/reservation.

### 11.4. Maintainability

- Controller chỉ nhận request, validate input cơ bản và gọi service.
- Business logic nằm trong service.
- Enum dùng cho status, không dùng string rải rác.
- DTO tách khỏi Entity.
- Dùng snapshot cho order item và booking service để bảo toàn dữ liệu lịch sử.

### 11.5. UI/UX

- Thymeleaf + Bootstrap.
- Customer dễ thấy:
  - Phụ tùng còn hàng.
  - Hình thức nhận hàng.
  - Tiền cọc cần thanh toán.
  - Số tiền còn lại dự kiến.
  - Slot còn trống.
  - Trạng thái đơn/lịch.
- Staff dashboard cần ưu tiên:
  - Đơn đã cọc đang chờ xử lý.
  - Lịch hẹn hôm nay.
  - Lịch quá giờ/no-show.
  - Phát sinh chờ khách duyệt.

---

## 12. Validation Rules

### 12.1. Product

```text
name required
sku required and unique
price >= 0
physical_stock >= 0
reserved_stock >= 0
reserved_stock <= physical_stock
status in ACTIVE, INACTIVE
```

### 12.2. Service

```text
name required
duration_minutes > 0
min_price >= 0
max_price >= min_price
status in ACTIVE, INACTIVE
```

### 12.3. Cart Item

```text
product_id required
quantity > 0
fulfillment_type in SHIPPING, AT_WORKSHOP
product must be ACTIVE
quantity <= available_stock at checkout time
```

### 12.4. Booking

```text
vehicle must belong to current customer
selected services must be ACTIVE
total_duration_minutes > 0
start_time < end_time
slot must have capacity
booking_date cannot be in the past
```

### 12.5. Payment

```text
amount > 0
payos_order_code unique
payment_deadline required
status must follow payment state machine
```

---

## 13. Suggested Java Enums

```java
public enum Role {
    CUSTOMER,
    STAFF,
    ADMIN
}

public enum ProductStatus {
    ACTIVE,
    INACTIVE
}

public enum ServiceStatus {
    ACTIVE,
    INACTIVE
}

public enum FulfillmentType {
    SHIPPING,
    AT_WORKSHOP
}

public enum OrderType {
    PARENT,
    SHIPPING,
    AT_WORKSHOP
}

public enum OrderStatus {
    CREATED,
    PENDING_DEPOSIT,
    DEPOSITED,
    PROCESSING,
    SHIPPING,
    COMPLETED,
    CANCELED,
    EXPIRED_PAYMENT
}

public enum BookingStatus {
    CREATED,
    PENDING_DEPOSIT,
    CONFIRMED,
    IN_PROGRESS,
    PENDING_APPROVAL,
    COMPLETED,
    CANCELED,
    EXPIRED_PAYMENT,
    EXPIRED_NO_SHOW
}

public enum PaymentStatus {
    INITIATED,
    PENDING,
    PAID,
    FAILED,
    CANCELED,
    EXPIRED,
    REFUNDED
}

public enum ReservationStatus {
    HELD,
    CONFIRMED,
    RELEASED,
    CONSUMED,
    EXPIRED
}

public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
```

---

## 14. Test Scenarios bắt buộc

### 14.1. Cart & checkout

- Cart chỉ có shipping item tạo đúng shipping order.
- Cart chỉ có workshop item bắt buộc chọn vehicle/service/slot.
- Cart mixed tạo parent order + 2 sub-orders + booking.
- Không cho checkout nếu product hết hàng.
- Không cho checkout nếu workshop item chưa chọn service/slot.

### 14.2. Deposit

- Shipping order tính đúng 20% product total.
- Booking tính đúng 20% service min total.
- O2O mixed tính đúng tổng cọc của product + service.
- Làm tròn tiền VND nhất quán.

### 14.3. Inventory

- Reserve stock khi payment pending.
- Release stock khi payment expired.
- Confirm reservation khi PayOS paid.
- Consume stock khi order completed.
- Không oversell khi hai checkout cùng sản phẩm cuối cùng.

### 14.4. Booking slot

- Tính đúng tổng duration nhiều service.
- Không hiển thị slot không đủ thời lượng liên tục.
- Không overbook quá capacity cầu nâng.
- Pending payment booking vẫn chiếm slot trong 15 phút.
- Expired payment giải phóng slot.

### 14.5. PayOS

- Return URL không tự set payment paid.
- Webhook hợp lệ set paid.
- Webhook sai checksum bị từ chối.
- Webhook lặp không cập nhật trùng.
- Payment expired trước webhook paid cần có rule xử lý rõ: nếu PayOS báo paid sau deadline, ưu tiên kiểm tra thực tế transaction. Với đồ án, có thể từ chối và yêu cầu staff xử lý thủ công, hoặc vẫn nhận nếu PayOS xác nhận paid trước thời điểm deadline.

### 14.6. RBAC

- Customer không vào được `/admin/**` và `/staff/**`.
- Customer A không xem được order/booking của Customer B.
- Staff không CRUD service/product nếu không có quyền Admin.
- Admin truy cập được dashboard và CRUD dữ liệu nền.

---

## 15. Scope cuối cùng

### 15.1. In Scope

- Authentication + RBAC với Spring Security.
- Customer product catalog.
- Customer service catalog.
- Vehicle profile.
- Smart cart có fulfillment type.
- Order splitting theo shipping/workshop.
- Dynamic booking slot theo duration và capacity.
- Deposit calculation 20%.
- PayOS Sandbox create payment link + return URL + webhook.
- Inventory reservation + optimistic locking.
- Order state machine.
- Booking state machine.
- Staff workflow xử lý đơn/lịch.
- Admin CRUD product/service/config cơ bản.
- Dashboard Chart.js:
  - Utilization rate.
  - Deposit revenue.
  - Forecast remaining cash.
  - Reserved inventory.
- Web notification nội bộ.

### 15.2. Out of Scope

- Kết nối ngân hàng thật bằng tài khoản doanh nghiệp.
- SMS/Email bên thứ ba.
- WebSocket realtime.
- Hoàn tiền tự động qua ngân hàng.
- Tối ưu route giao hàng thực tế.
- Quản lý nhiều chi nhánh/xưởng phức tạp.
- Mobile app native.

---

## 16. Agent Implementation Notes

Khi coding agent sinh code, cần tuân thủ các nguyên tắc sau:

1. Không đặt business logic trong Controller.
2. Không cập nhật trực tiếp stock trong nhiều service khác nhau.
3. Không tin `returnUrl` của PayOS là thanh toán thành công.
4. Webhook phải verify checksum và idempotent.
5. Mọi entity có owner như order, booking, vehicle phải kiểm tra ownership.
6. `available_stock` là computed value, không phải cột chính cần cập nhật thủ công.
7. Pending payment vẫn phải giữ kho và giữ slot để tránh overselling/overbooking.
8. Booking service và order item phải lưu snapshot giá/tên/thời lượng.
9. Tất cả tiền tệ dùng `BigDecimal`.
10. Các trạng thái phải đi qua workflow service, không update status trực tiếp từ controller.
11. Với SQL Server, dùng `DATETIME2` cho timestamp.
12. Với Thymeleaf form, bật CSRF token.
13. Với concurrency, dùng `@Version` và transaction boundary rõ ràng.
14. Với mixed checkout, payment transaction nên trỏ về parent order và booking liên quan.
15. Khi hủy/hết hạn, luôn release reservation nếu chưa consumed.

---

## 17. Definition of Done

Một chức năng được xem là hoàn thành khi:

- Có UI Thymeleaf cơ bản để thao tác.
- Có validation ở form/request DTO.
- Có kiểm tra phân quyền/ownership.
- Có service xử lý nghiệp vụ.
- Có transaction ở các thao tác nhiều bước.
- Có thông báo lỗi rõ ràng cho user.
- Có test scenario thủ công hoặc unit/integration test tương ứng.
- Không phá vỡ state machine.
- Không làm lệch stock/reservation.
- Không phụ thuộc vào return URL để xác nhận thanh toán.

---

## 18. Luồng triển khai khuyến nghị theo giai đoạn

### Phase 1 — Foundation

- User, Role, Spring Security.
- Product CRUD.
- Service CRUD.
- Vehicle profile.
- Basic layout Thymeleaf + Bootstrap.

### Phase 2 — Cart & Order

- Product catalog.
- Smart cart.
- Checkout shipping only.
- Order state machine cơ bản.
- Inventory reservation cơ bản.

### Phase 3 — Booking

- Service selection.
- Dynamic slot generation.
- Booking state machine.
- Workshop order flow.

### Phase 4 — O2O Mixed Checkout

- Parent order.
- Sub-order splitting.
- Booking linked with workshop order.
- Deposit calculation tổng hợp.

### Phase 5 — PayOS

- Create payment link sandbox.
- Return URL UI.
- Webhook verify checksum.
- Idempotent payment handling.
- Expire pending payment job.

### Phase 6 — Staff/Admin Dashboard

- Staff xử lý đơn/lịch.
- Phát sinh sửa chữa.
- Customer approve/reject extras.
- Admin dashboard Chart.js.

---

## 19. Kết luận nghiệp vụ

Hệ thống có logic khả thi cho đồ án Spring Boot MVC nếu chuẩn hóa ba điểm quan trọng:

1. **Giữ kho và giữ slot ngay từ trạng thái `PENDING_DEPOSIT`**, không chờ đến lúc thanh toán xong.
2. **Tách PaymentTransaction riêng** để xử lý PayOS sạch, nhất là với luồng O2O mixed.
3. **Quản lý trạng thái qua workflow service** để tránh cập nhật sai vòng đời đơn hàng/lịch hẹn.

Phiên bản tài liệu này đã điều chỉnh các mâu thuẫn nghiệp vụ chính và có thể dùng làm file mô tả cho agent đọc trước khi sinh code.
