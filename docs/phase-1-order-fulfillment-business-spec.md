# Giai đoạn 1 — Nền tảng đơn hàng giao hàng và lắp đặt tại xưởng

## 1. Mục tiêu

Chuẩn bị dữ liệu và quy tắc nền tảng để hệ thống xử lý đúng ba loại giỏ hàng:

- `SHIPPING`: phụ tùng được giao đến khách.
- `AT_WORKSHOP`: phụ tùng được giữ tại xưởng để lắp cho xe của khách.
- `MIXED`: giỏ có cả hai hình thức; khi checkout sẽ tạo đơn tổng và các đơn con ở giai đoạn sau.

Giai đoạn này **chưa thay đổi toàn bộ checkout hoặc giao diện**. Mục tiêu là tạo mô hình dữ liệu an toàn, có thể mở rộng, và không làm hỏng booking sửa chữa hiện tại.

## 2. Quy tắc nghiệp vụ đã chốt

### 2.1 Giao hàng

- Khách chỉ thanh toán qua PayOS; COD bị loại bỏ.
- Địa chỉ giao hàng gồm: tỉnh/thành phố, quận/huyện, phường/xã và địa chỉ chi tiết.
- Phí ship được cấu hình bởi Admin theo tỉnh/thành phố và quận/huyện.
- Nếu khu vực chưa có cấu hình phí đang hoạt động, khách không được đặt đơn giao hàng.
- Phí ship được chụp tại thời điểm tạo đơn; thay đổi cấu hình sau đó không làm đổi phí của đơn cũ.
- Staff/Admin xử lý vận chuyển thủ công bằng đơn vị vận chuyển và mã vận đơn.

### 2.2 Lắp đặt tại xưởng

- Chỉ sản phẩm có `installationSupported = true` mới được chọn lắp đặt.
- Khách chọn xe, ngày và slot giờ; không chọn dịch vụ sửa chữa/bảo dưỡng.
- Khách thanh toán online tiền phụ tùng. Tiền công chưa bao gồm và được thông báo ở giỏ, checkout và chi tiết đơn.
- Staff xác định tiền công thực tế sau khi thực hiện xong, sau đó ghi nhận đã thu tại xưởng bằng tiền mặt hoặc chuyển khoản ngoài hệ thống.
- Không thể hoàn tất lịch lắp đặt khi chưa có tiền công cuối cùng và chưa xác nhận đã thu.

### 2.3 Booking

- Booking dùng chung entity hiện tại nhưng có `bookingType`:
  - `REPAIR_SERVICE`: luồng sửa chữa/bảo dưỡng cũ; vẫn yêu cầu ít nhất một dịch vụ.
  - `PART_INSTALLATION`: lịch lắp phụ tùng; không yêu cầu service.
- Hai loại booking dùng chung sức chứa slot xưởng.
- Booking lắp đặt chỉ hoàn tất cùng order liên quan sau khi staff ghi nhận tiền công và đã thu tiền.

## 3. Dữ liệu cần bổ sung

### 3.1 Product

| Trường | Kiểu | Quy tắc |
| --- | --- | --- |
| `installation_supported` | boolean | Mặc định `false`; chỉ sản phẩm `true` mới cho phép chọn `AT_WORKSHOP`. |

### 3.2 Order

| Trường | Kiểu | Quy tắc |
| --- | --- | --- |
| `shipping_fee` | decimal | Không âm; bằng 0 với order không giao hàng. |
| `shipping_province` | text | Bắt buộc với order giao hàng. |
| `shipping_district` | text | Bắt buộc với order giao hàng. |
| `shipping_ward` | text | Bắt buộc với order giao hàng. |
| `shipping_address` | text | Địa chỉ chi tiết; chỉ bắt buộc với order giao hàng. |

`shipping_address` phải cho phép `NULL` đối với `AT_WORKSHOP` và `PARENT`; validation ở service sẽ quyết định khi nào trường này bắt buộc.

### 3.3 Booking

| Trường | Kiểu | Quy tắc |
| --- | --- | --- |
| `booking_type` | enum | `REPAIR_SERVICE` hoặc `PART_INSTALLATION`. |
| `labor_fee` | decimal | Chỉ áp dụng cho `PART_INSTALLATION`; phải không âm khi hoàn tất. |
| `labor_collected` | boolean | Mặc định `false`. |
| `labor_collected_at` | datetime | Có giá trị khi `labor_collected = true`. |
| `labor_collected_by_id` | user FK | Staff/Admin ghi nhận thu tiền. |

### 3.4 ShippingFeeRule

Tạo bảng cấu hình phí ship:

| Trường | Kiểu | Quy tắc |
| --- | --- | --- |
| `province` | text | Bắt buộc. |
| `district` | text | Bắt buộc. |
| `fee` | decimal | Không âm. |
| `active` | boolean | Chỉ rule active mới được dùng khi checkout. |

Tổ hợp `province + district` phải duy nhất.

## 4. Trạng thái và trách nhiệm

```text
Giao hàng: PROCESSING → SHIPPING → COMPLETED

Lắp đặt: PAYMENT SUCCESS → Booking CONFIRMED → Thực hiện tại xưởng
          → Nhập tiền công → Đã thu tiền công → Booking/Order COMPLETED
```

- Customer: chọn hình thức nhận, thanh toán PayOS, gửi yêu cầu hủy/hoàn tiền.
- Staff/Admin: cấu hình phí ship, nhập vận đơn, cập nhật trạng thái, nhập và xác nhận thu tiền công.
- Payment module: xử lý link thanh toán và payout/refund PayOS; nằm ngoài phạm vi Giai đoạn 1.

## 5. Phạm vi triển khai Giai đoạn 1

### Bao gồm

- Migration/schema cho các trường mới.
- Entity, enum, repository cơ bản cho phí ship.
- Loại bỏ COD khỏi model thanh toán dùng cho checkout/order.
- Validation/domain foundation để phân biệt booking sửa chữa và booking lắp đặt.
- Tests cho các quy tắc domain mới.

### Không bao gồm

- Form admin quản lý phí ship.
- UI đổi fulfillment trong giỏ hàng.
- Checkout tính phí ship, tách parent/sub-order và tạo booking lắp đặt.
- Staff nhập vận đơn hoặc nhập tiền công.
- Refund/payout PayOS.

## 6. Tiêu chí nghiệm thu Giai đoạn 1

- Product có thể được đánh dấu có/không hỗ trợ lắp đặt; mặc định không hỗ trợ.
- Booking sửa chữa vẫn yêu cầu service theo logic cũ.
- Booking lắp đặt được phân biệt bằng `bookingType`, không dựa vào `service = null`.
- Schema cho phép order xưởng không có địa chỉ giao hàng.
- Rule phí ship chỉ chấp nhận một cấu hình active cho một tỉnh/quận.
- Không còn `COD` trong enum và model checkout/order.
- Main source build thành công.
- Test mới cho quy tắc domain chạy được sau khi khắc phục cấu hình test classpath hiện có.

## 7. Rủi ro và cách kiểm soát

| Rủi ro | Kiểm soát |
| --- | --- |
| Làm hỏng booking sửa chữa | Dùng `bookingType`; không bỏ validation service của `REPAIR_SERVICE`. |
| Database cũ vẫn ép `shipping_address NOT NULL` | Migration phải thay đổi nullability trước khi checkout mới được dùng. |
| Phí ship thay đổi làm sai đơn cũ | Lưu snapshot `shipping_fee` trực tiếp trên Order. |
| COD còn sót tại template/controller | Giai đoạn 1 tìm và loại khỏi enum, DTO, service; UI checkout được xử lý hoàn chỉnh ở Giai đoạn 2. |
