# Tài liệu chức năng dự án GearShift Pro / CarShowRoom

## 1. Mục đích hệ thống

GearShift Pro là hệ thống showroom/xưởng ô tô cho phép khách hàng:

- Đặt lịch dịch vụ bảo dưỡng/sửa chữa.
- Mua phụ tùng trực tuyến.
- Chọn nhận hàng giao tận nhà hoặc lắp đặt tại xưởng.
- Thanh toán trực tuyến, theo dõi đơn/lịch và yêu cầu hủy.
- Nhận hoàn tiền theo quy trình nhân viên xác nhận chuyển khoản thủ công.

Hệ thống có ba nhóm người dùng chính: **Khách hàng**, **Nhân viên** và **Quản trị viên**.

## 2. Chức năng cho khách hàng

### 2.1. Tài khoản

- Đăng ký, đăng nhập và đăng xuất.
- Xem/cập nhật hồ sơ cá nhân.
- Đổi mật khẩu.
- Quản lý xe cá nhân: thêm xe, xem danh sách xe, xóa xe.

### 2.2. Danh mục phụ tùng

- Xem cửa hàng phụ tùng, danh mục và chi tiết từng sản phẩm.
- Hiển thị tên, SKU, giá, tồn kho, mô tả, ảnh URL và khả năng hỗ trợ lắp tại xưởng.
- Sản phẩm có thể liên kết với dòng xe tương thích.

### 2.3. Giỏ hàng và kiểm soát tồn kho

- Thêm sản phẩm vào giỏ.
- Cập nhật số lượng hoặc xóa sản phẩm.
- Mỗi lần thêm/cập nhật, hệ thống cộng tổng số lượng của cùng sản phẩm trong giỏ để kiểm tra tồn kho.
- Kiểm tra tồn kho lại khi khách vào thanh toán để tránh tình trạng tồn kho đã bị khách khác mua.
- Thông báo lỗi nghiệp vụ bằng tiếng Việt, không hiển thị lỗi hệ thống.
- Mỗi lượt thanh toán chỉ hỗ trợ **một hình thức nhận hàng**: giao tận nhà hoặc lắp đặt tại xưởng.

### 2.4. Đặt hàng giao tận nhà

- Nhập thông tin liên hệ, số điện thoại và địa chỉ nhận hàng.
- Chọn tỉnh/thành phố, quận/huyện, phường/xã và địa chỉ chi tiết.
- Phí giao hàng được tính theo quy tắc tỉnh/thành phố và quận/huyện do quản trị viên cấu hình.
- Hiển thị phí giao hàng dự kiến và tổng tiền trước khi tạo thanh toán.
- Hỗ trợ thanh toán trực tuyến hoặc COD đối với đơn giao hàng.
- Với thanh toán trực tuyến, hệ thống tạo giao dịch PayOS và cập nhật đơn theo kết quả thanh toán.

### 2.5. Đặt hàng lắp đặt tại xưởng

- Chỉ áp dụng cho sản phẩm đã bật **Hỗ trợ lắp đặt tại xưởng**.
- Khách chọn xe đã có hoặc tạo xe mới.
- Dịch vụ lắp đặt được cố định là **Thay thế phụ tùng**.
- Hiển thị khoảng phí thay thế, thời lượng và tiền cọc cần thanh toán.
- Chọn ngày/khung giờ còn chỗ tại xưởng.
- Cần thanh toán tiền cọc trực tuyến để giữ phụ tùng và khung giờ.
- Phần chi phí còn lại được thanh toán tại xưởng sau khi hoàn thành lắp đặt.

### 2.6. Đặt lịch dịch vụ

- Chọn xe, dịch vụ, ngày hẹn, khung giờ và ghi chú tình trạng xe.
- Giá dịch vụ hiển thị theo khoảng giá thấp–cao.
- Tính tiền cọc theo tỷ lệ, mức tối thiểu và mức tối đa do quản trị viên cấu hình.
- Chỉ hiển thị các khung giờ hợp lệ: không trùng ngày nghỉ, không trong giờ nghỉ trưa, đủ thời gian đặt trước và chưa vượt sức chứa xưởng.
- Thanh toán tiền cọc trực tuyến để xác nhận lịch hẹn.
- Xem lịch sử và chi tiết lịch hẹn, bao gồm trạng thái, dịch vụ, xe, tiền cọc và thanh toán còn lại.

### 2.7. Theo dõi đơn hàng/lịch hẹn

- Xem lịch sử đơn phụ tùng và chi tiết đơn.
- Xem lịch sử đặt lịch và chi tiết lịch.
- Lịch sử hiển thị trạng thái xử lý, trạng thái hoàn tiền và nút thao tác phù hợp.
- Lịch sử đặt lịch gộp ngày và giờ hẹn thành một cột để dễ thao tác.

## 3. Thanh toán

### 3.1. Thanh toán PayOS

- Tạo giao dịch thanh toán trực tuyến cho đơn hàng/lịch hẹn cần thanh toán trước.
- Nhận kết quả từ URL trả về, webhook hoặc thao tác đồng bộ giao dịch.
- Có xử lý hết hạn thanh toán/giữ hàng theo thời gian cấu hình.
- Khi thanh toán thành công, cập nhật trạng thái thanh toán và trạng thái đơn/lịch tương ứng.

### 3.2. COD

- Chỉ dành cho đơn giao tận nhà.
- Không cho chọn COD cho đơn có lắp đặt tại xưởng vì loại đơn này cần tiền cọc trực tuyến.

## 4. Hủy và hoàn tiền

### 4.1. Hủy lịch hẹn đã thanh toán cọc

1. Khách gửi yêu cầu hủy lịch.
2. Lịch chưa bị hủy ngay mà chuyển sang chờ nhân viên duyệt.
3. Admin/nhân viên duyệt hoặc từ chối yêu cầu.
4. Nếu duyệt, lịch chuyển sang đã hủy và tạo yêu cầu hoàn cọc.
5. Khách nhập ngân hàng, tên người nhận và số tài khoản nhận tiền.
6. Nhân viên chuyển khoản thủ công, nhập mã giao dịch và xác nhận đã hoàn tiền.
7. Hệ thống cập nhật: lịch đã hủy, thanh toán đã hoàn tiền, hoàn tiền đã hoàn.

Chính sách hoàn cọc:

- Hủy trước số giờ cấu hình: hoàn toàn bộ tiền cọc.
- Hủy sát giờ: hoàn theo tỷ lệ cấu hình.
- Khách vắng mặt: hoàn theo tỷ lệ vắng mặt cấu hình.
- Căn cứ tính tiền dùng **thời điểm khách gửi yêu cầu hủy**, không dùng thời điểm nhân viên duyệt.
- Admin thấy tiền cọc, tỷ lệ áp dụng, số tiền phải chuyển và lý do tính hoàn trước khi xác nhận.

### 4.2. Hủy đơn hàng đã thanh toán

1. Khách gửi yêu cầu hủy đơn còn đủ điều kiện hủy.
2. Đơn chuyển sang chờ duyệt; nhân viên không tiếp tục xử lý giao hàng trong thời gian này.
3. Admin/nhân viên duyệt hoặc từ chối.
4. Nếu duyệt, khách nhập thông tin nhận tiền hoàn.
5. Nhân viên chuyển khoản thủ công, nhập mã giao dịch và xác nhận hoàn tiền.
6. Lịch sử đơn hàng của khách hiển thị rõ: chờ duyệt, chờ hoàn, đã hoàn, lỗi hoàn hoặc bị từ chối.

### 4.3. Dữ liệu hoàn tiền được lưu

- Số tiền hoàn.
- Lý do/căn cứ hoàn tiền.
- Trạng thái yêu cầu hoàn tiền.
- Hạn xử lý hoàn tiền.
- Tài khoản nhận tiền của khách.
- Mã giao dịch chuyển khoản.
- Người xác nhận và thời điểm hoàn.

## 5. Chức năng cho nhân viên

- Xem dashboard nhân viên.
- Xem danh sách và chi tiết lịch hẹn.
- Xem danh sách phụ tùng/đơn hàng.
- Cập nhật trạng thái đơn hàng theo luồng cho phép.
- Tiếp nhận xe (check-in), cập nhật giá cuối cùng và ghi nhận tiền công lắp đặt.
- Đánh dấu khách vắng mặt theo thời gian chờ cấu hình.
- Duyệt/từ chối yêu cầu hủy đơn hoặc lịch.
- Xác nhận hoàn tiền thủ công bằng mã giao dịch.
- Đồng bộ trạng thái giao dịch hoàn tiền khi có giao dịch đang xử lý.

## 6. Chức năng cho quản trị viên

### 6.1. Dashboard và người dùng

- Xem số liệu tổng quan đơn hàng, lịch hẹn, người dùng và doanh thu.
- Xem danh sách người dùng.
- Tạo, chỉnh sửa người dùng và thay đổi trạng thái tài khoản.

### 6.2. Danh mục và phụ tùng

- Quản lý danh mục phụ tùng.
- Quản lý dòng xe: thêm, sửa, xóa.
- Quản lý sản phẩm: thêm, sửa, xóa, bật/tắt trạng thái.
- Quản lý SKU, giá, tồn kho, URL ảnh, mô tả, dòng xe tương thích.
- Bật/tắt hỗ trợ lắp đặt tại xưởng cho từng sản phẩm.

### 6.3. Dịch vụ

- Tạo, sửa, xóa dịch vụ.
- Cấu hình mô tả, khoảng giá, thời lượng và trạng thái hoạt động.

### 6.4. Quản lý đơn hàng và vận chuyển

- Xem danh sách/chi tiết đơn.
- Cập nhật trạng thái đơn.
- Cập nhật đơn vị vận chuyển và mã vận đơn cho đơn giao tận nhà.
- Xem số tiền hoàn, lý do hoàn, thông tin tài khoản và xác nhận hoàn tiền.

### 6.5. Quản lý lịch hẹn

- Xem danh sách/chi tiết lịch.
- Cập nhật trạng thái lịch phù hợp.
- Duyệt/từ chối yêu cầu hủy.
- Xem chính sách hoàn được áp dụng cho từng lịch.
- Ghi nhận tiền công lắp đặt tại xưởng.
- Xác nhận hoàn cọc sau khi nhân viên đã chuyển khoản.

### 6.6. Cấu hình hệ thống

Các cấu hình có thể thay đổi từ trang quản trị:

- Giờ bắt đầu/kết thúc làm việc.
- Giờ nghỉ trưa.
- Số xe tối đa xưởng phục vụ đồng thời.
- Bước tạo khung giờ.
- Thời gian đặt lịch trước tối thiểu.
- Thời gian giữ hàng/slot chờ thanh toán.
- Tỷ lệ cọc dịch vụ, cọc tối thiểu và cọc tối đa.
- Số giờ hủy miễn phí để hoàn toàn bộ cọc.
- Tỷ lệ hoàn cọc khi hủy sát giờ.
- Thời gian chờ để tính khách vắng mặt và tỷ lệ hoàn trong trường hợp vắng mặt.
- Thời hạn xử lý hoàn tiền.
- Ngày xưởng nghỉ và lý do nghỉ.
- Lịch sử thay đổi cấu hình.

### 6.7. Phí giao hàng

- Tạo/sửa quy tắc phí giao hàng theo tỉnh/thành phố và quận/huyện.
- Bật/tắt từng quy tắc phí.
- Phí được dùng để dự đoán tổng tiền lúc khách checkout và chốt vào đơn hàng.

## 7. Các trạng thái nghiệp vụ chính

| Nhóm | Một số trạng thái |
|---|---|
| Đơn hàng | Chờ thanh toán, Đang xử lý, Đang giao, Hoàn tất, Đã hủy, Hết hạn thanh toán |
| Lịch hẹn | Chờ thanh toán, Đã xác nhận, Đang chờ xe, Đang tiếp nhận xe, Đang thực hiện, Hoàn tất, Đã hủy, Hết hạn |
| Thanh toán | Chờ thanh toán, Đã thanh toán, Đã hoàn tiền, Hết hạn, Đã hủy |
| Hoàn tiền | Không có, Chờ duyệt/yêu cầu, Đã duyệt, Đang xử lý, Đã hoàn, Từ chối, Lỗi |

## 8. Kiểm tra an toàn nghiệp vụ

- Không cho đặt lịch trong quá khứ hoặc quá sát giờ cấu hình.
- Không cho chọn slot đã đầy, ngày nghỉ hoặc thời gian nghỉ trưa.
- Không cho vượt quá tồn kho khi thêm/cập nhật giỏ hoặc checkout.
- Không cho hủy/lặp yêu cầu hủy khi trạng thái không còn phù hợp.
- Không cho hoàn tiền nếu khách chưa cung cấp đủ ngân hàng, tên người nhận và số tài khoản.
- Tên người nhận hoàn tiền phải khớp với tên khách đã thanh toán.
- Không cho hoàn tất hoàn tiền nếu thiếu mã giao dịch chuyển khoản.
- Database có migration tự sửa các dữ liệu demo cũ bị lỗi tiếng Việt trong tên sản phẩm của đơn hàng.

## 9. Công nghệ và cách chạy

- Backend: Java 21, Spring Boot, Spring MVC, Spring Data JPA, Spring Security.
- Database: Microsoft SQL Server.
- Giao diện: Thymeleaf, Bootstrap và CSS tùy chỉnh.
- Thanh toán: PayOS cho thanh toán trực tuyến.

Chạy dự án tại thư mục gốc:

```powershell
mvn spring-boot:run
```

Ứng dụng chạy mặc định tại: `http://localhost:8386`.

Kiểm thử:

```powershell
mvn test
```

