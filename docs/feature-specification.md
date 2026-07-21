# GearShift Pro – Feature Specification

## 1. Mục đích

Tài liệu này mô tả trạng thái các chức năng hiện có trong source code của GearShift Pro.

- **Hoàn thiện**: đã có luồng xử lý và giao diện chính trong source code.
- **Một phần**: đã có nền tảng nhưng còn giới hạn, cần kiểm thử hoặc bổ sung nghiệp vụ.
- **Chưa hoàn thiện**: chưa có hoặc chưa đủ để nghiệm thu.

## 2. Tổng quan trạng thái

| Module | Trạng thái | Ghi chú |
| --- | --- | --- |
| Đăng ký, đăng nhập, phân quyền | Hoàn thiện | Customer, Staff, Admin |
| Hồ sơ khách hàng | Hoàn thiện | Cập nhật hồ sơ và mật khẩu |
| Danh mục phụ tùng | Hoàn thiện | Xem, tìm kiếm, lọc, phân trang |
| Giỏ hàng | Hoàn thiện | Thêm, sửa số lượng, xóa, kiểm tra tồn kho |
| Đặt hàng phụ tùng | Hoàn thiện | Checkout, lịch sử, trạng thái |
| Thanh toán PayOS | Một phần | Có tích hợp link/webhook; cần kiểm thử tài khoản PayOS thật |
| Quản lý xe | Hoàn thiện | Thêm, xem, xóa xe của khách |
| Đặt lịch dịch vụ | Hoàn thiện | Chọn xe, dịch vụ, ngày, khung giờ, tiền cọc |
| Hoàn tiền đặt lịch | Một phần | Có form và thao tác admin/staff; việc chuyển tiền thực tế vẫn thủ công |
| Hoàn tiền đơn hàng | Một phần | Có yêu cầu và xác nhận hoàn; chuyển tiền thực tế vẫn thủ công |
| Quản trị Admin | Hoàn thiện | Dashboard và CRUD chính |
| Vận hành Staff | Hoàn thiện | Xử lý đơn hàng, lịch hẹn, hoàn tiền |

## 3. Chức năng Customer

### 3.1 Tài khoản và bảo mật

- [x] Đăng ký bằng email, mật khẩu, họ tên, số điện thoại, địa chỉ.
- [x] Đăng nhập và đăng xuất.
- [x] Mật khẩu được mã hóa BCrypt.
- [x] Chặn tài khoản bị khóa.
- [x] Phân quyền Customer, Staff, Admin.
- [x] Xem và cập nhật hồ sơ.
- [x] Đổi mật khẩu.

### 3.2 Phụ tùng và giỏ hàng

- [x] Xem danh sách phụ tùng đang hoạt động.
- [x] Xem chi tiết sản phẩm.
- [x] Tìm kiếm theo từ khóa.
- [x] Lọc theo danh mục.
- [x] Phân trang danh sách sản phẩm.
- [x] Thêm sản phẩm vào giỏ.
- [x] Cập nhật số lượng.
- [x] Xóa sản phẩm khỏi giỏ.
- [x] Kiểm tra số lượng tồn kho.
- [x] Tính tổng tiền giỏ hàng.

### 3.3 Đặt hàng phụ tùng

- [x] Nhập địa chỉ giao hàng.
- [x] Tạo đơn hàng từ giỏ hàng.
- [x] Tạo chi tiết đơn hàng.
- [x] Trừ tồn kho khi checkout.
- [x] Xóa giỏ hàng sau khi tạo đơn.
- [x] Xem lịch sử đơn hàng.
- [x] Xem chi tiết đơn hàng.
- [x] Thanh toán đơn hàng qua PayOS.
- [x] Thanh toán lại đơn chưa thanh toán.
- [x] Hủy đơn trước khi chuyển sang trạng thái giao hàng theo nghiệp vụ hiện tại.
- [~] Hoàn tiền đơn đã thanh toán: có yêu cầu hoàn và thao tác admin/staff, nhưng chuyển khoản hoàn tiền vẫn thao tác thủ công.

### 3.4 Quản lý xe

- [x] Xem danh sách xe của khách hàng.
- [x] Thêm xe bằng dòng xe và biển số.
- [x] Cho phép nhập xe mới nếu chưa có trong hệ thống.
- [x] Chỉ cho phép khách sử dụng xe thuộc tài khoản của mình.
- [x] Xóa xe của chính khách hàng.

### 3.5 Đặt lịch dịch vụ

- [x] Chọn xe bắt buộc trước khi đặt lịch.
- [x] Cho phép thêm xe mới từ luồng booking.
- [x] Chọn dịch vụ đang hoạt động.
- [x] Hiển thị giá dịch vụ theo khoảng thấp–cao.
- [x] Chọn ngày hẹn, không cho chọn ngày trong quá khứ.
- [x] Hiển thị các khung giờ còn chỗ.
- [x] Kiểm tra giới hạn số xe trong một khung giờ.
- [x] Nhập ghi chú tình trạng xe.
- [x] Tạo booking ở trạng thái chờ thanh toán.
- [x] Tự động chuyển booking sang xác nhận sau khi thanh toán tiền cọc thành công.
- [x] Hiển thị tiền cọc trước khi đặt lịch.
- [x] Tiền cọc bằng 20% giá dịch vụ dự kiến.
- [x] Tiền cọc tối thiểu 2.000 VNĐ.
- [x] Tiền cọc tối đa 10.000 VNĐ.
- [x] Thời hạn thanh toán tiền cọc là 15 phút.
- [x] Tự giải phóng booking hết hạn thanh toán theo cơ chế xử lý trạng thái.
- [x] Xem lịch sử đặt lịch.
- [x] Xem chi tiết booking.
- [x] Hủy booking đủ điều kiện.
- [x] Khi booking đã thanh toán bị hủy hợp lệ, tạo yêu cầu hoàn cọc.
- [x] Nhập ngân hàng, số tài khoản nhận hoàn cọc.
- [x] Tên người nhận bị khóa theo người đã thanh toán tiền cọc.
- [x] Backend kiểm tra tên người nhận phải trùng người thanh toán.
- [~] Hoàn tiền đặt cọc: admin/staff xác nhận thủ công sau khi thực hiện chuyển tiền bên ngoài hệ thống.

## 4. Chức năng Staff

- [x] Truy cập dashboard riêng.
- [x] Xem dữ liệu đơn hàng vận hành.
- [x] Cập nhật trạng thái đơn hàng.
- [x] Xem dữ liệu lịch hẹn.
- [x] Cập nhật trạng thái lịch hẹn.
- [x] Xem yêu cầu hoàn tiền đơn hàng.
- [x] Xem yêu cầu hoàn tiền booking.
- [x] Nhập mã giao dịch và xác nhận đã hoàn tiền.
- [~] Chuyển tiền thực tế: chưa tự động qua ngân hàng, cần thao tác ngoài hệ thống.

## 5. Chức năng Admin

### 5.1 Dashboard

- [x] Tổng số người dùng.
- [x] Tổng số sản phẩm.
- [x] Tổng số đơn hàng.
- [x] Tổng số booking.
- [x] Doanh thu đơn hàng.
- [x] Doanh thu dịch vụ.
- [x] Tổng doanh thu.
- [x] Danh sách đơn hàng và booking gần đây.
- [x] Dữ liệu biểu đồ doanh thu theo tháng.

### 5.2 Quản trị dữ liệu

- [x] Quản lý người dùng: xem, tìm kiếm, tạo, sửa, đổi trạng thái.
- [x] Quản lý danh mục phụ tùng: xem, tạo, sửa, xóa.
- [x] Quản lý sản phẩm: xem, tạo, sửa, xóa, đổi trạng thái.
- [x] Quản lý dòng xe và năm sản xuất.
- [x] Quản lý dịch vụ: xem, tạo, sửa, xóa.

### 5.3 Đơn hàng và booking

- [x] Xem danh sách đơn hàng.
- [x] Xem chi tiết đơn hàng.
- [x] Cập nhật trạng thái đơn hàng.
- [x] Xác nhận thông tin giao hàng và mã vận đơn.
- [x] Xử lý yêu cầu hoàn tiền đơn hàng.
- [x] Xem danh sách booking.
- [x] Xem chi tiết booking.
- [x] Cập nhật trạng thái booking.
- [x] Hiển thị nút hoàn tiền cho booking đã hủy và đang chờ hoàn cọc.
- [x] Xem lịch sử giao dịch thanh toán của booking.
- [x] Xem thông tin tài khoản nhận hoàn do khách cung cấp.
- [x] Xác nhận đã hoàn tiền và ghi chú/mã giao dịch.
- [~] Hoàn tiền tự động: chưa tích hợp API chuyển khoản ngân hàng.

## 6. Thanh toán

- [x] Tạo payment link PayOS cho đơn hàng.
- [x] Tạo payment link PayOS cho tiền cọc booking.
- [x] Xử lý URL trả về thành công/thất bại.
- [x] Xử lý webhook PayOS.
- [x] Đồng bộ trạng thái giao dịch.
- [x] Lưu lịch sử giao dịch thanh toán.
- [x] Trạng thái giao dịch: chờ thanh toán, đã thanh toán, hoàn tiền.
- [~] Môi trường production: cần cấu hình và kiểm thử đầy đủ PayOS credentials thật.
- [ ] Tự động chuyển tiền hoàn vào tài khoản ngân hàng khách hàng.

## 7. Chức năng chưa hoàn thiện hoặc cần bổ sung

- [ ] Tích hợp API chuyển tiền hoàn tự động.
- [ ] Gửi email/SMS xác nhận booking, thanh toán và hoàn tiền.
- [ ] Cho phép khách theo dõi tiến trình hoàn tiền chi tiết.
- [ ] Báo cáo doanh thu nâng cao và xuất Excel/PDF.
- [ ] Quản lý nhà cung cấp, nhập hàng và purchase order.
- [ ] Quản lý nhiều chi nhánh showroom.
- [ ] Tìm kiếm phụ tùng tương thích theo dòng xe trong giao diện.
- [ ] Viết bộ test tự động cho controller, service và payment webhook.
- [ ] Kiểm thử end-to-end đầy đủ với tài khoản Customer, Staff và Admin.

## 8. Tiêu chí nghiệm thu đề xuất

Một module được xem là hoàn thiện khi:

1. Người dùng đúng vai trò truy cập được page tương ứng.
2. Luồng thành công và luồng lỗi đều có thông báo giao diện rõ ràng.
3. Dữ liệu được lưu đúng vào database.
4. Không thể truy cập hoặc chỉnh sửa dữ liệu của người dùng khác.
5. Trạng thái nghiệp vụ được cập nhật đúng sau thao tác.
6. Có kiểm thử thủ công tối thiểu cho các trường hợp chính.

## 9. Lưu ý triển khai

- Sau khi thay đổi source code, cần khởi động lại ứng dụng Spring Boot.
- Ứng dụng chạy local mặc định trên port `8386`.
- Các thay đổi schema booking/payment được xử lý bởi migration runner khi ứng dụng khởi động.
- File này phản ánh trạng thái source code hiện tại, không thay thế kết quả kiểm thử trên môi trường production.

## 10. Đặc tả chi tiết theo route

### 11.1 Public và Authentication

| Route | Actor | Chức năng | Input chính | Kết quả |
| --- | --- | --- | --- | --- |
| `GET /` | Guest | Trang chủ | Không có | Hiển thị trang chủ |
| `GET /shop` | Guest | Danh sách phụ tùng | `categoryId`, từ khóa, trang | Hiển thị sản phẩm đang hoạt động |
| `GET /products/{id}` | Guest | Chi tiết sản phẩm | `id` sản phẩm | Hiển thị tên, giá, tồn kho, ảnh, mô tả |
| `GET /auth/register` | Guest | Form đăng ký | Không có | Hiển thị form |
| `POST /auth/register` | Guest | Tạo tài khoản | Email, mật khẩu, họ tên, điện thoại, địa chỉ | Tạo Customer hoặc trả lỗi validation |
| `GET /auth/login` | Guest | Form đăng nhập | Không có | Hiển thị form |
| `POST /auth/login` | Guest | Đăng nhập | Email, mật khẩu | Tạo session và chuyển hướng theo role |
| `POST /auth/logout` | User | Đăng xuất | Session/CSRF | Hủy session và về trang đăng nhập |

### 11.2 Account

| Route | Actor | Trạng thái | Mô tả |
| --- | --- | --- | --- |
| `GET /account` | Customer | Hoàn thiện | Hiển thị hồ sơ, số đơn hàng, số booking và liên kết tài khoản |
| `POST /account/profile` | Customer | Hoàn thiện | Cập nhật họ tên, số điện thoại, địa chỉ |
| `POST /account/password` | Customer | Hoàn thiện | Đổi mật khẩu với kiểm tra mật khẩu hiện tại |

### 11.3 Cart và Order

| Route | Actor | Input | Kiểm tra nghiệp vụ | Kết quả |
| --- | --- | --- | --- | --- |
| `GET /cart` | Customer | Không có | Người dùng phải đăng nhập | Hiển thị giỏ và tổng tiền |
| `POST /cart/add` | Customer | `productId`, `quantity` | Sản phẩm active, quantity > 0, đủ tồn kho | Thêm hoặc cộng dồn sản phẩm |
| `POST /cart/update` | Customer | `cartItemId`, `quantity` | Item thuộc customer, đủ tồn kho | Cập nhật số lượng |
| `POST /cart/remove` | Customer | `cartItemId` | Item thuộc customer | Xóa item |
| `GET /orders/checkout` | Customer | Không có | Giỏ không rỗng | Hiển thị form checkout |
| `POST /orders/checkout` | Customer | Địa chỉ giao hàng | Cart hợp lệ, đủ tồn kho | Tạo order, detail, trừ tồn kho, xóa cart |
| `GET /orders` | Customer | Không có | Chỉ xem order của mình | Danh sách lịch sử đơn hàng |
| `GET /orders/{id}` | Customer | `id` | Order phải thuộc customer | Chi tiết đơn hàng |
| `GET /orders/{id}/payment` | Customer | `id` | Order còn chờ thanh toán | Trang thanh toán lại |
| `POST /orders/{id}/payment` | Customer | `id` | Order hợp lệ | Tạo link PayOS |
| `POST /orders/{id}/cancel` | Customer | `id` | Chưa giao hàng, chưa hoàn tất | Hủy order hoặc tạo yêu cầu hoàn tiền |
| `POST /orders/{id}/shipping-address` | Customer | Địa chỉ mới | Order còn cho phép cập nhật | Lưu địa chỉ mới |

### 11.4 Booking và lịch hẹn

| Route | Actor | Input | Kiểm tra nghiệp vụ | Kết quả |
| --- | --- | --- | --- | --- |
| `GET /booking` | Customer | Không có | Đã đăng nhập | Form tạo lịch |
| `GET /booking/create` | Customer | Không có | Alias của `/booking` | Form tạo lịch |
| `POST /booking` | Customer | Xe, dịch vụ, ngày, giờ, ghi chú | Xe bắt buộc và thuộc customer; service active; ngày hợp lệ; slot còn chỗ | Tạo `PENDING_PAYMENT`, tính cọc, tạo PayOS |
| `GET /booking/available-slots` | Customer | `date`, `serviceId` | Ngày/service hợp lệ | Danh sách slot còn chỗ |
| `GET /booking/my-bookings` | Customer | Không có | Chỉ xem booking của mình | Lịch sử booking và thao tác |
| `GET /booking/{id}` | Customer | `id` | Booking phải thuộc customer | Chi tiết, dịch vụ, tiền cọc, refund |
| `GET /booking/{id}/payment` | Customer | `id` | Booking `PENDING_PAYMENT` | Trang thanh toán lại |
| `POST /booking/{id}/payment` | Customer | `id` | Booking hợp lệ | Tạo lại link PayOS |
| `POST /booking/{id}/cancel` | Customer | `id` | Không đang làm, hoàn tất, hủy hoặc hết hạn | Đặt `CANCELED`; nếu đã trả cọc thì tạo refund request |
| `POST /booking/{id}/refund-account` | Customer | Ngân hàng, tên, số tài khoản | Booking đã hủy và đã trả cọc; tên phải trùng người trả tiền | Lưu tài khoản nhận hoàn |

#### Quy tắc tiền cọc

```text
Tiền cọc = 20% giá thấp nhất dự kiến của dịch vụ
Tiền cọc tối thiểu = 2.000 VNĐ
Tiền cọc tối đa = 10.000 VNĐ
Thời hạn giữ slot để thanh toán = 15 phút
```

| Giá thấp nhất | 20% | Tiền cọc thực tế |
| ---: | ---: | ---: |
| 5.000 VNĐ | 1.000 VNĐ | 2.000 VNĐ |
| 30.000 VNĐ | 6.000 VNĐ | 6.000 VNĐ |
| 80.000 VNĐ | 16.000 VNĐ | 10.000 VNĐ |

#### Vòng đời booking

```text
PENDING_PAYMENT --thanh toán thành công--> CONFIRMED
CONFIRMED ------bắt đầu thực hiện-------> IN_PROGRESS
IN_PROGRESS ----hoàn tất dịch vụ--------> COMPLETED
PENDING_PAYMENT/CONFIRMED --------------> CANCELED
```

Booking đã thanh toán nếu bị hủy hợp lệ sẽ đi theo luồng:

```text
CANCELED + Payment PAID
        -> RefundStatus REQUESTED
        -> Customer nhập tài khoản nhận hoàn
        -> Admin/Staff chuyển tiền thủ công
        -> RefundStatus COMPLETED + Payment REFUNDED
```

### 11.5 Vehicle

| Route | Actor | Input | Kiểm tra | Kết quả |
| --- | --- | --- | --- | --- |
| `GET /vehicles` | Customer | Không có | Đã đăng nhập | Danh sách xe của user |
| `GET /vehicles/add` | Customer | Không có | Đã đăng nhập | Form thêm xe |
| `POST /vehicles/add` | Customer | `carModelId`, biển số | Dòng xe tồn tại, biển số hợp lệ | Tạo xe |
| `POST /vehicles/{id}/delete` | Customer | `id` | Xe phải thuộc user | Xóa xe |

## 11. Đặc tả trạng thái nghiệp vụ

### 12.1 PaymentStatus

| Trạng thái | Ý nghĩa | Chuyển tiếp |
| --- | --- | --- |
| `PENDING` | Chưa thanh toán | `PAID` hoặc hết hạn/hủy |
| `PAID` | PayOS đã xác nhận thanh toán | `REFUNDED` nếu hoàn tiền hợp lệ |
| `REFUNDED` | Đã ghi nhận hoàn tiền | Trạng thái cuối |

### 12.2 RefundStatus

| Trạng thái | Điều kiện | Người thao tác |
| --- | --- | --- |
| `NONE` | Chưa phát sinh yêu cầu hoàn | Hệ thống |
| `REQUESTED` | Customer hủy order/booking đã thanh toán | Hệ thống |
| `COMPLETED` | Admin/Staff đã chuyển tiền và nhập mã giao dịch | Admin/Staff |

### 12.3 OrderStatus

| Trạng thái | Diễn giải |
| --- | --- |
| `PENDING` | Đơn mới tạo, chờ xử lý |
| `PROCESSING` | Đang chuẩn bị hàng |
| `SHIPPED` | Đã giao cho đơn vị vận chuyển |
| `DELIVERED` | Đã giao thành công |
| `CANCELLED` | Đơn đã hủy |

### 12.4 BookingStatus

| Trạng thái | Diễn giải |
| --- | --- |
| `PENDING_PAYMENT` | Đã giữ slot tạm thời, chưa thanh toán cọc |
| `CONFIRMED` | Đã thanh toán cọc |
| `IN_PROGRESS` | Đang thực hiện dịch vụ |
| `COMPLETED` | Hoàn thành dịch vụ |
| `CANCELED` | Đã hủy |
| `EXPIRED_PAYMENT` | Quá hạn thanh toán cọc |
| `EXPIRED_NO_SHOW` | Khách không đến theo lịch |

## 12. Ma trận quyền truy cập

| Chức năng | Guest | Customer | Staff | Admin |
| --- | :---: | :---: | :---: | :---: |
| Xem trang chủ/catalog | Có | Có | Có | Có |
| Quản lý giỏ hàng | Không | Có | Không | Không |
| Đặt hàng | Không | Có | Không | Không |
| Đặt lịch dịch vụ | Không | Có | Không | Không |
| Quản lý xe | Không | Có | Không | Không |
| Xem order/booking của bản thân | Không | Có | Không | Không |
| Cập nhật order vận hành | Không | Không | Có | Có |
| Cập nhật booking vận hành | Không | Không | Có | Có |
| Hoàn tiền order/booking | Không | Không | Có | Có |
| CRUD sản phẩm/dịch vụ | Không | Không | Không | Có |
| Quản lý user | Không | Không | Không | Có |
| Xem dashboard doanh thu | Không | Không | Không | Có |

## 13. Đặc tả Admin và Staff

### Admin

- [x] Dashboard số user, sản phẩm, đơn hàng, booking và doanh thu.
- [x] Xem đơn hàng/booking gần đây.
- [x] Quản lý user: tạo, sửa, xem chi tiết, tìm kiếm và khóa/mở khóa.
- [x] Quản lý category: tạo, sửa, xóa, xem danh sách.
- [x] Quản lý product: tạo, sửa, xóa, đổi trạng thái.
- [x] Quản lý service: tạo, sửa, xóa, xem danh sách.
- [x] Quản lý car model và năm sản xuất.
- [x] Xem danh sách và chi tiết booking.
- [x] Cập nhật trạng thái booking.
- [x] Xem lịch sử giao dịch thanh toán booking.
- [x] Xem thông tin tài khoản nhận hoàn do khách nhập.
- [x] Xác nhận hoàn tiền booking với ghi chú/mã giao dịch.
- [x] Xử lý hoàn tiền order.
- [~] Chuyển tiền thực tế: vẫn thực hiện bên ngoài hệ thống.

### Staff

- [x] Dashboard riêng.
- [x] Xem order và booking cần xử lý.
- [x] Cập nhật trạng thái order.
- [x] Cập nhật trạng thái booking.
- [x] Xử lý hoàn tiền order.
- [x] Xử lý hoàn tiền booking.
- [~] Chuyển tiền thực tế: chưa tích hợp API ngân hàng.

## 14. Thanh toán và hoàn tiền

### Thanh toán PayOS

- [x] Tạo payment link cho order.
- [x] Tạo payment link cho tiền cọc booking.
- [x] Có trang thanh toán lại cho giao dịch chưa hoàn tất.
- [x] Có URL return thành công và thất bại.
- [x] Có webhook PayOS.
- [x] Đồng bộ trạng thái giao dịch.
- [x] Lưu payment transaction và mã PayOS order code.
- [~] Cần kiểm thử bằng PayOS credentials thật trước khi nghiệm thu production.

### Hoàn tiền

- [x] Customer hủy order đã trả tiền trước khi giao thì phát sinh yêu cầu hoàn.
- [x] Customer hủy booking đã trả cọc hợp lệ thì phát sinh yêu cầu hoàn cọc.
- [x] Customer nhập ngân hàng và số tài khoản nhận tiền.
- [x] Tên tài khoản nhận hoàn tự động lấy theo người trả tiền.
- [x] Backend chặn tên người nhận khác người thanh toán.
- [x] Admin/Staff xem thông tin tài khoản hoàn.
- [x] Admin/Staff nhập ghi chú hoặc mã giao dịch hoàn.
- [x] Sau khi xác nhận, giao dịch chuyển sang `REFUNDED`.
- [ ] Tự động chuyển khoản ngân hàng.
- [ ] Đối soát tự động với ngân hàng.

## 15. Danh sách lỗi và trường hợp cần kiểm thử

### Customer

- [ ] Truy cập `/booking/my-bookings` khi chưa đăng nhập phải chuyển về login, không trả 500.
- [ ] Gửi booking không có xe phải bị từ chối.
- [ ] Gửi booking bằng xe của user khác phải bị từ chối.
- [ ] Đặt ngày trong quá khứ phải bị từ chối.
- [ ] Đặt slot đã đầy phải bị từ chối.
- [ ] Thanh toán quá 15 phút phải làm booking hết hạn và giải phóng slot.
- [ ] Customer không được xem order/booking của customer khác.
- [ ] Nhập tên nhận hoàn khác tên người trả tiền phải bị từ chối ở backend.
- [ ] Hủy booking đã hoàn tất phải bị từ chối.
- [ ] Hủy order đã giao phải bị từ chối.

### Admin/Staff

- [ ] User không đúng role không được truy cập `/admin/**` hoặc `/staff/**`.
- [ ] Không được hoàn booking chưa thanh toán.
- [ ] Không được hoàn booking chưa ở trạng thái hủy.
- [ ] Không được hoàn một booking đã hoàn trước đó.
- [ ] Thiếu ngân hàng, số tài khoản hoặc mã giao dịch phải bị từ chối.
- [ ] Chủ tài khoản khác khách trả cọc phải bị từ chối.
- [ ] Sau khi hoàn, payment transaction phải có trạng thái `REFUNDED`.

## 16. Bảng kiểm nghiệm thu

| Hạng mục | Đã code | Đã kiểm thử đầy đủ | Ghi chú |
| --- | :---: | :---: | --- |
| Đăng ký/đăng nhập | Có | Chưa | Kiểm tra role và user bị khóa |
| Catalog/tìm kiếm | Có | Chưa | Kiểm tra lọc và phân trang |
| Cart/checkout | Có | Chưa | Kiểm tra tồn kho |
| PayOS order | Có | Chưa | Cần PayOS thật |
| PayOS booking | Có | Chưa | Cần kiểm tra webhook |
| Booking slot capacity | Có | Chưa | Kiểm tra slot đầy và đặt đồng thời |
| Hủy booking/refund request | Có | Chưa | Kiểm tra trạng thái PAID |
| Form tài khoản hoàn tiền | Có | Chưa | Kiểm tra tên người nhận |
| Admin/Staff hoàn tiền | Có | Chưa | Chuyển tiền thực tế thủ công |
| Lịch sử giao dịch | Có | Chưa | Kiểm tra giao dịch refund |
| Customer tabbar | Có | Chưa | Kiểm tra responsive |

## 17. File code chính

| Nhóm | File |
| --- | --- |
| Booking controller | `src/main/java/com/hsf302/carshowroom/controller/BookingController.java` |
| Booking service | `src/main/java/com/hsf302/carshowroom/service/impl/BookingServiceImpl.java` |
| Order service | `src/main/java/com/hsf302/carshowroom/service/impl/OrderServiceImpl.java` |
| Payment service | `src/main/java/com/hsf302/carshowroom/service/impl/PaymentServiceImpl.java` |
| Admin controller | `src/main/java/com/hsf302/carshowroom/controller/AdminController.java` |
| Staff controller | `src/main/java/com/hsf302/carshowroom/controller/StaffController.java` |
| Booking history | `src/main/resources/templates/booking/my-bookings.html` |
| Booking detail | `src/main/resources/templates/booking/detail.html` |
| Admin booking list | `src/main/resources/templates/admin/booking/list.html` |
| Admin booking detail | `src/main/resources/templates/admin/booking/detail.html` |
| Customer navigation | `src/main/resources/templates/fragments/sidebar.html` |
| Schema migration | `src/main/java/com/hsf302/carshowroom/config/SchemaMigrationRunner.java` |

## 18. Kết luận

Phần lõi đã có đủ các luồng: đăng nhập, phụ tùng, giỏ hàng, đặt hàng, quản lý xe, booking, thanh toán, admin, staff và hoàn tiền thủ công.

Các phần chưa thể xem là hoàn thiện production:

1. Tự động chuyển tiền hoàn vào tài khoản ngân hàng.
2. Đối soát hoàn tiền tự động.
3. Kiểm thử PayOS bằng credentials thật.
4. Automated tests cho phân quyền, webhook, hết hạn tiền cọc và hoàn tiền.
5. Email/SMS xác nhận giao dịch và booking.

## 19. Phân công task cho nhóm

### 21.1 Bảng phân công tổng quan

| Thành viên | Phụ trách chính | Module |
| --- | --- | --- |
| Huy | Booking, tiền cọc và hoàn tiền | Customer booking, PayOS booking, refund booking |
| Trường | Phụ tùng, giỏ hàng và đơn hàng | Catalog, cart, checkout, order |
| Nam | Admin và dữ liệu hệ thống | Admin dashboard, CRUD, database/migration |
| Anh | Staff, authentication và security | Staff workflow, login, role, authorization |
| Ngân | UI/UX, kiểm thử và tài liệu | Customer layout, responsive, test checklist, specification |

### 21.2 Huy – Booking, tiền cọc và hoàn tiền

#### Task chính

- [x] Hoàn thiện form tạo booking.
- [x] Bắt buộc khách chọn xe trước khi đặt lịch.
- [x] Cho phép thêm xe mới từ luồng booking.
- [x] Chọn dịch vụ, ngày hẹn và khung giờ.
- [x] Kiểm tra sức chứa khung giờ.
- [x] Tính tiền cọc 20%, tối thiểu 2.000 VNĐ, tối đa 10.000 VNĐ.
- [x] Tạo link thanh toán PayOS cho tiền cọc.
- [x] Xử lý booking hết hạn thanh toán sau 15 phút.
- [x] Xây dựng lịch sử booking và trang chi tiết booking.
- [x] Xử lý hủy booking hợp lệ.
- [x] Tạo yêu cầu hoàn tiền cọc.
- [x] Thêm form nhập ngân hàng, số tài khoản và tên người nhận.
- [x] Kiểm tra tên người nhận phải trùng người đã thanh toán cọc.
- [x] Thêm thao tác hoàn tiền cho Admin/Staff.

#### Task cần hoàn thiện

- [ ] Kiểm thử toàn bộ booking bằng dữ liệu thật.
- [ ] Kiểm thử đặt đồng thời cùng một khung giờ.
- [ ] Hoàn thiện xử lý tự động slot sau khi hết hạn thanh toán.
- [ ] Tích hợp API chuyển khoản hoàn tiền tự động nếu có yêu cầu.

#### File phụ trách

- `BookingController.java`
- `BookingService.java`
- `BookingServiceImpl.java`
- `Booking.java`
- `SchedulingServiceImpl.java`
- `booking/create.html`
- `booking/my-bookings.html`
- `booking/detail.html`
- `admin/booking/detail.html`

### 21.3 Trường – Phụ tùng, giỏ hàng và đơn hàng

#### Task chính

- [x] Hiển thị danh mục phụ tùng.
- [x] Tìm kiếm và lọc sản phẩm.
- [x] Hiển thị chi tiết sản phẩm.
- [x] Thêm sản phẩm vào giỏ hàng.
- [x] Cập nhật số lượng sản phẩm.
- [x] Xóa sản phẩm khỏi giỏ.
- [x] Kiểm tra tồn kho.
- [x] Tạo checkout.
- [x] Tạo order và order detail.
- [x] Trừ tồn kho sau checkout.
- [x] Xóa giỏ hàng sau khi tạo order.
- [x] Hiển thị lịch sử và chi tiết đơn hàng.
- [x] Thanh toán order qua PayOS.
- [x] Hủy order trước khi giao hàng.
- [x] Tạo yêu cầu hoàn tiền order.

#### Task cần hoàn thiện

- [ ] Kiểm thử quantity lớn hơn tồn kho.
- [ ] Kiểm thử checkout khi hai user mua cùng sản phẩm.
- [ ] Kiểm thử thanh toán lại order hết hạn.
- [ ] Hoàn thiện giao diện trạng thái order bằng tiếng Việt.
- [ ] Kiểm tra responsive cho cart, checkout và order detail.

#### File phụ trách

- `CartController.java`
- `OrderController.java`
- `OrderService.java`
- `OrderServiceImpl.java`
- `ProductController.java`
- `cart/*.html`
- `order/*.html`
- `shop/*.html`

### 21.4 Nam – Admin và dữ liệu hệ thống

#### Task chính

- [x] Dashboard Admin.
- [x] Thống kê user, product, order, booking và doanh thu.
- [x] Quản lý user.
- [x] Quản lý category.
- [x] Quản lý product.
- [x] Quản lý service.
- [x] Quản lý car model.
- [x] Xem danh sách order và booking.
- [x] Xem chi tiết order và booking.
- [x] Cập nhật trạng thái order và booking.
- [x] Hiển thị nút hoàn tiền booking.
- [x] Hiển thị lịch sử giao dịch thanh toán.
- [x] Nhận thông tin tài khoản hoàn tiền từ customer.

#### Task cần hoàn thiện

- [ ] Kiểm tra quyền Admin cho tất cả route quản trị.
- [ ] Kiểm tra validation các form CRUD.
- [ ] Kiểm thử xóa product/category/service có dữ liệu liên quan.
- [ ] Bổ sung bộ lọc nâng cao cho booking và order.
- [ ] Bổ sung báo cáo hoặc export doanh thu nếu cần.
- [ ] Kiểm tra migration trên database mới và database cũ.

#### File phụ trách

- `AdminController.java`
- `SchemaMigrationRunner.java`
- `DataSeeder.java`
- `admin/*.html`
- Các repository và entity quản trị.

### 21.5 Anh – Staff, authentication và security

#### Task chính

- [x] Đăng ký customer.
- [x] Đăng nhập và đăng xuất.
- [x] Mã hóa mật khẩu bằng BCrypt.
- [x] Chặn user bị khóa.
- [x] Điều hướng theo role.
- [x] Bảo vệ route Customer, Staff và Admin.
- [x] Dashboard Staff.
- [x] Cập nhật trạng thái order từ Staff.
- [x] Cập nhật trạng thái booking từ Staff.
- [x] Xử lý hoàn tiền order từ Staff.
- [x] Xử lý hoàn tiền booking từ Staff.

#### Task cần hoàn thiện

- [ ] Kiểm thử truy cập trái quyền bằng từng role.
- [ ] Kiểm thử session sau logout.
- [ ] Kiểm tra CSRF cho các form POST.
- [ ] Kiểm tra tài khoản bị khóa không thể đăng nhập.
- [ ] Chuẩn hóa thông báo lỗi đăng nhập/đăng ký bằng tiếng Việt.
- [ ] Bổ sung automated security tests.

#### File phụ trách

- `AuthController.java`
- `AuthService.java`
- `AuthServiceImpl.java`
- `SecurityConfig.java`
- `GlobalControllerAdvice.java`
- `StaffController.java`
- `staff/*.html`

### 21.6 Ngân – UI/UX, kiểm thử và tài liệu

#### Task chính

- [x] Đồng bộ thanh tab ngang cho các page customer.
- [x] Mở rộng chiều rộng nội dung customer.
- [x] Bổ sung nút nhập thông tin hoàn tiền tại lịch sử booking.
- [x] Việt hóa trạng thái booking và các nút thao tác chính.
- [x] Cập nhật form refund customer và admin.
- [x] Tạo file feature specification.
- [x] Tạo checklist nghiệm thu.

#### Task cần hoàn thiện

- [ ] Kiểm tra giao diện desktop, tablet và mobile.
- [ ] Kiểm tra các page customer sau khi đổi tabbar.
- [ ] Kiểm tra lỗi Thymeleaf trên tất cả page.
- [ ] Kiểm tra hiển thị tiếng Việt không bị lỗi encoding.
- [ ] Viết test case thủ công cho Customer, Staff và Admin.
- [ ] Chụp ảnh minh chứng cho các chức năng đã nghiệm thu.
- [ ] Cập nhật specification sau mỗi thay đổi nghiệp vụ.

#### File phụ trách

- `templates/fragments/header.html`
- `templates/fragments/sidebar.html`
- `templates/account/*.html`
- `templates/booking/*.html`
- `templates/order/*.html`
- `templates/vehicles/*.html`
- `docs/feature-specification.md`

### 19.7 Bổ sung image sản phẩm

Đây là task bổ sung vì catalog hiện tại còn thiếu hình ảnh cho nhiều sản phẩm.

| Phần việc | Người phụ trách | Kết quả cần bàn giao |
| --- | --- | --- |
| Kiểm kê sản phẩm chưa có ảnh | Trường | Danh sách product ID, tên sản phẩm và trạng thái ảnh |
| Chuẩn bị/chọn ảnh phù hợp | Ngân | Bộ ảnh sản phẩm đã đổi tên, đúng kích thước và không vi phạm bản quyền |
| Cập nhật `imageUrl` hoặc dữ liệu seed | Nam | Database/seed data trỏ đúng tới từng ảnh |
| Hiển thị ảnh trong catalog và detail | Trường | Card sản phẩm và trang chi tiết hiển thị đúng ảnh |
| Fallback khi ảnh lỗi hoặc thiếu | Anh | Ảnh mặc định, không làm vỡ layout khi URL không tồn tại |
| Kiểm thử và nghiệm thu | Huy | Kiểm tra ảnh trên shop, product detail, cart/order và mobile |

#### Chi tiết task theo thành viên

##### Trường – Backend/catalog

- [ ] Kiểm tra toàn bộ sản phẩm đang thiếu `imageUrl`.
- [ ] Chuẩn hóa đường dẫn ảnh theo một quy ước duy nhất.
- [ ] Cập nhật ảnh trong seed data hoặc form quản lý product.
- [ ] Kiểm tra ảnh xuất hiện đúng trong `/shop` và `/products/{id}`.

##### Ngân – Chuẩn bị tài nguyên và giao diện

- [ ] Tạo thư mục ảnh sản phẩm dùng chung.
- [ ] Đặt tên ảnh theo mã/tên sản phẩm, không dùng ký tự đặc biệt.
- [ ] Chuẩn hóa định dạng ưu tiên `.webp` hoặc `.jpg`.
- [ ] Kiểm tra ảnh có cùng tỷ lệ để card sản phẩm không bị lệch.
- [ ] Bổ sung ảnh mặc định nếu sản phẩm chưa có ảnh thật.

##### Nam – Database và Admin

- [ ] Kiểm tra cột lưu đường dẫn ảnh trong bảng product.
- [ ] Cập nhật dữ liệu sản phẩm hiện có.
- [ ] Bổ sung trường upload/chọn ảnh trong form Admin nếu cần.
- [ ] Đảm bảo migration/seed không ghi đè sai đường dẫn ảnh.

##### Anh – Fallback và quyền truy cập

- [ ] Kiểm tra ảnh không tồn tại phải dùng ảnh mặc định.
- [ ] Kiểm tra đường dẫn ảnh không cho phép truy cập file ngoài thư mục static hợp lệ.
- [ ] Kiểm tra Admin mới được phép thay đổi dữ liệu ảnh sản phẩm.

##### Huy – Kiểm thử nghiệm thu

- [ ] Kiểm tra sản phẩm có ảnh ở trang shop.
- [ ] Kiểm tra trang chi tiết sản phẩm.
- [ ] Kiểm tra ảnh không bị mất khi thêm vào cart.
- [ ] Kiểm tra ảnh trong order detail nếu có hiển thị.
- [ ] Kiểm tra trên desktop, tablet và mobile.

#### Tiêu chí hoàn thành image sản phẩm

- [ ] 100% sản phẩm active có ảnh hoặc ảnh mặc định.
- [ ] Không có ảnh bị lỗi `404`.
- [ ] Không có ảnh bị kéo méo hoặc làm vỡ layout.
- [ ] Ảnh hiển thị đúng sản phẩm ở catalog và detail.
- [ ] Đường dẫn ảnh hoạt động sau khi build/package và khởi động lại ứng dụng.
- [ ] Có kiểm tra fallback khi xóa hoặc đổi tên một file ảnh.

## 20. Quy tắc phối hợp nhóm

1. Mỗi thành viên chỉ sửa các module chính được phân công, trừ khi có thống nhất chung.
2. Khi thay đổi entity hoặc database, phải báo cho Nam trước khi merge.
3. Khi thay đổi route hoặc trạng thái nghiệp vụ, phải báo cho Huy và Anh để kiểm tra luồng và quyền truy cập.
4. Khi thay đổi template dùng chung, phải báo cho Ngân để kiểm tra giao diện các page còn lại.
5. Trường hợp thay đổi thanh toán hoặc hoàn tiền phải được kiểm tra chéo bởi Huy và Nam.
6. Mỗi task hoàn thành phải cập nhật checklist trong file này.
7. Trước khi nộp bài, cả nhóm chạy:

```powershell
git diff --check
./mvnw.cmd -q -DskipTests compile
```

8. Không commit password, PayOS secret key hoặc thông tin tài khoản ngân hàng thật vào repository.
