package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.FulfillmentType;
import com.hsf302.carshowroom.common.Enums.BookingStatus;
import com.hsf302.carshowroom.common.Enums.BookingType;
import com.hsf302.carshowroom.common.Enums.OrderStatus;
import com.hsf302.carshowroom.common.Enums.OrderType;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.common.Enums.PaymentMethod;
import com.hsf302.carshowroom.common.Enums.RefundPayoutStatus;
import com.hsf302.carshowroom.common.Enums.RefundStatus;
import com.hsf302.carshowroom.common.Enums.ServiceStatus;
import com.hsf302.carshowroom.dto.CheckoutForm;
import com.hsf302.carshowroom.dto.CheckoutResult;
import com.hsf302.carshowroom.dto.PayOS.PayOSCreatePaymentLinkRequest;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.OrderItem;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import com.hsf302.carshowroom.entity.RefundTransaction;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.entity.Vehicle;
import com.hsf302.carshowroom.exception.MixedFulfillmentException;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.BookingServiceRepository;
import com.hsf302.carshowroom.repository.CartItemRepository;
import com.hsf302.carshowroom.repository.OrderItemRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.repository.PaymentTransactionRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.CartInventoryValidationService;
import com.hsf302.carshowroom.service.InventoryReservationService;
import com.hsf302.carshowroom.service.OrderService;
import com.hsf302.carshowroom.service.OrderWorkflowService;
import com.hsf302.carshowroom.service.PaymentService;
import com.hsf302.carshowroom.service.RefundPayoutService;
import com.hsf302.carshowroom.service.SchedulingService;
import com.hsf302.carshowroom.service.RefundService;
import com.hsf302.carshowroom.service.ShippingFeeService;
import com.hsf302.carshowroom.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final String WORKSHOP_INSTALLATION_SERVICE = "Thay thế phụ tùng";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final ServiceRepository serviceRepository;
    private final InventoryReservationService inventoryReservationService;
    private final CartInventoryValidationService cartInventoryValidationService;
    private final SchedulingService schedulingService;
    private final PaymentService paymentService;
    private final OrderWorkflowService orderWorkflowService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundService refundService;
    private final RefundPayoutService refundPayoutService;
    private final ShippingFeeService shippingFeeService;
    private final SystemSettingService settingService;

    /** Tạo đơn từ giỏ hàng, giữ tồn kho và tạo lịch hẹn khi khách chọn lắp tại xưởng. */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    /**
     * Tạo đơn từ giỏ hàng trong giao dịch cô lập để tránh bán vượt tồn kho.
     * Đơn lắp tại xưởng sẽ tạo thêm Booking với dịch vụ cố định và giữ khung giờ.
     */
    public CheckoutResult checkout(User user, CheckoutForm form) {
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        validateSingleFulfillment(cartItems);
        validateCartStock(cartItems);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống.");
        }

        cartInventoryValidationService.validateCheckoutStock(cartItems);

        Map<FulfillmentType, List<CartItem>> groupedItems = cartItems.stream()
                .collect(Collectors.groupingBy(item -> item.getFulfillmentType() == null
                        ? FulfillmentType.SHIPPING
                        : item.getFulfillmentType()));

        List<CartItem> shippingItems = groupedItems.getOrDefault(FulfillmentType.SHIPPING, List.of());
        List<CartItem> workshopItems = groupedItems.getOrDefault(FulfillmentType.AT_WORKSHOP, List.of());
        if (workshopItems.stream().anyMatch(item -> !item.getProduct().isInstallationSupported())) {
            throw new IllegalStateException("Có sản phẩm trong giỏ không hỗ trợ lắp đặt tại xưởng.");
        }
        boolean mixed = !shippingItems.isEmpty() && !workshopItems.isEmpty();
        PaymentMethod paymentMethod = parsePaymentMethod(form.getPaymentMethod());
        ShippingDetails shippingDetails = shippingItems.isEmpty() ? null : resolveShippingDetails(form);

        Order parentOrder = mixed ? createOrder(user, OrderType.PARENT, null, null, form.getPhone(), List.of()) : null;
        List<Order> stockOrders = new ArrayList<>();
        Booking booking = null;

        if (!shippingItems.isEmpty()) {
            stockOrders.add(createOrder(user, OrderType.SHIPPING, parentOrder, shippingDetails, form.getPhone(), shippingItems));
        }
        if (!workshopItems.isEmpty()) {
            validateWorkshopCheckout(form);
            Order workshopOrder = createOrder(user, OrderType.AT_WORKSHOP, parentOrder, null, form.getPhone(), workshopItems);
            stockOrders.add(workshopOrder);
            booking = createWorkshopBooking(user, form, workshopOrder);
        }

        if (parentOrder != null) {
            BigDecimal total = stockOrders.stream().map(Order::getProductTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            parentOrder.setProductTotal(total);
            if (booking != null) {
                booking.setFinalAmount(booking.getEstimatedMinAmount());
            }
            parentOrder.setPaymentStatus(PaymentStatus.PENDING);
            parentOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
            parentOrder = orderRepository.save(parentOrder);
        }

        List<Order> allOrders = new ArrayList<>(stockOrders);
        if (parentOrder != null) {
            allOrders.add(parentOrder);
        }
        allOrders.forEach(order -> order.setPaymentMethod(paymentMethod));

        inventoryReservationService.reserveStock(stockOrders);
        cartItemRepository.deleteByUser(user);
        Integer confirmationOrderId = parentOrder != null ? parentOrder.getId() : stockOrders.get(0).getId();
        PaymentTransaction transaction = createPayOSPayment(user, parentOrder, stockOrders, booking);
        return new CheckoutResult(confirmationOrderId, transaction.getCheckoutUrl());
    }

    /** Lấy danh sách đơn hàng của khách theo thời điểm tạo giảm dần. */
    @Override
    public List<Order> getOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /** Lấy đơn hàng theo mã cho các thao tác nội bộ hoặc quản trị. */
    @Override
    public Order getOrderById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));
    }

    /** Lấy đơn hàng theo mã sau khi kiểm tra quyền sở hữu của khách hàng. */
    @Override
    public Order getOrderForUser(Integer id, User user) {
        Order order = getOrderById(id);
        assertOrderOwner(order, user);
        return order;
    }

    /** Hủy đơn theo yêu cầu khách, tạo hoàn tiền khi cần và hủy các đơn con. */
    @Override
    @Transactional
    public void cancelOrderForUser(Integer id, User user, String reason) {
        Order order = getOrderForUser(id, user);
        if (!isCustomerCancelable(order)) {
            throw new IllegalStateException("Không thể hủy đơn khi đơn đã được giao, hoàn tất hoặc đã hủy.");
        }
        order.setCancellationReason(requireText(reason, "Vui lòng nhập lý do hủy đơn."));
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            refundService.requestOrderRefund(order, order.getCancellationReason());
            order.setRefundStatus(RefundStatus.REQUESTED);
        }
        cancelOrderAndChildren(order);
        orderRepository.save(order);
    }

    /** Nhân viên duyệt yêu cầu hủy đơn đã được khách gửi và chuyển đơn sang chờ hoàn tiền. */
    @Override
    @Transactional
    public void approveCancellation(Integer id, User processedBy) {
        Order order = getOrderById(id);
        if (order.getRefundStatus() != RefundStatus.REQUESTED) {
            throw new IllegalStateException("Đơn hàng không có yêu cầu hủy đang chờ duyệt.");
        }
        order.setRefundStatus(order.getPaymentStatus() == PaymentStatus.PAID
                ? RefundStatus.APPROVED : RefundStatus.NONE);
        order.setCancellationProcessedBy(processedBy);
        order.setCancellationProcessedAt(LocalDateTime.now());
        order.setCancellationDecisionNote("Đã duyệt yêu cầu hủy đơn.");
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void rejectCancellation(Integer id, User processedBy, String reason) {
        Order order = getOrderById(id);
        if (order.getRefundStatus() != RefundStatus.REQUESTED) {
            throw new IllegalStateException("Đơn hàng không có yêu cầu hủy đang chờ duyệt.");
        }
        order.setRefundStatus(RefundStatus.REJECTED);
        order.setCancellationProcessedBy(processedBy);
        order.setCancellationProcessedAt(LocalDateTime.now());
        order.setCancellationDecisionNote(reason == null ? null : reason.trim());
        order.setRefundNote(requireText(reason, "Vui lòng nhập lý do từ chối."));
        orderRepository.save(order);
    }

    /** Cho khách sửa địa chỉ và số điện thoại nhận hàng khi đơn giao hàng còn được phép sửa. */
    @Override
    @Transactional
    public void updateShippingAddressForUser(Integer id, User user, String shippingAddress, String receiverPhone) {
        Order order = getOrderForUser(id, user);
        if (order.getOrderType() != OrderType.SHIPPING) {
            throw new IllegalStateException("Chỉ đơn giao hàng mới có thể cập nhật địa chỉ nhận hàng.");
        }
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT && order.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new IllegalStateException("Không thể sửa địa chỉ khi đơn đã được giao hoặc hoàn tất.");
        }
        order.setShippingAddress(requireText(shippingAddress, "Địa chỉ nhận hàng không được để trống."));
        order.setReceiverPhone(requireText(receiverPhone, "Số điện thoại nhận hàng không được để trống."));
        orderRepository.save(order);
    }

    /** Cập nhật đơn vị vận chuyển, mã vận đơn và chuyển đơn sang đang giao khi cần. */
    @Override
    @Transactional
    public void updateShipment(Integer id, String shippingCarrier, String trackingCode) {
        Order order = getOrderById(id);
        if (order.getOrderType() != OrderType.SHIPPING) {
            throw new IllegalStateException("Chỉ đơn giao hàng mới có mã vận đơn.");
        }
        if (order.getOrderStatus() != OrderStatus.PROCESSING
                && order.getOrderStatus() != OrderStatus.SHIPPING) {
            throw new IllegalStateException("Chỉ có thể cập nhật vận chuyển cho đơn đang xử lý hoặc đang giao.");
        }
        String validatedCarrier = requireText(shippingCarrier, "Vui lòng nhập đơn vị vận chuyển.");
        String validatedTrackingCode = requireText(trackingCode, "Vui lòng nhập mã vận đơn.");
        order.setShippingCarrier(validatedCarrier);
        order.setTrackingCode(validatedTrackingCode);
        if (order.getOrderStatus() == OrderStatus.PROCESSING) {
            orderWorkflowService.shipOrder(order);
        }
        orderRepository.save(order);
    }

    /** Nhân viên xác nhận hoàn tiền cho đơn hàng đã hủy và đồng bộ giao dịch thanh toán. */
    @Override
    @Transactional
    public void completeRefund(Integer id, User processedBy, String bankName, String bankBin,
                               String accountHolder, String accountNumber, String note) {
        Order order = getOrderById(id);
        if (order.getOrderStatus() != OrderStatus.CANCELED
                || (order.getRefundStatus() != RefundStatus.APPROVED && order.getRefundStatus() != RefundStatus.FAILED)
                || order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Đơn hàng không có yêu cầu hoàn tiền đang chờ xử lý.");
        }
        order.setRefundBankName(requireText(bankName, "Vui lòng nhập ngân hàng nhận hoàn tiền."));
        order.setRefundBankBin(requireText(bankBin, "Vui lòng nhập mã BIN ngân hàng nhận hoàn tiền."));
        order.setRefundAccountHolder(requireText(accountHolder, "Vui lòng nhập tên chủ tài khoản nhận hoàn tiền."));
        order.setRefundAccountNumber(requireText(accountNumber, "Vui lòng nhập số tài khoản nhận hoàn tiền."));
        order.setRefundNote(requireText(note, "Vui lòng nhập ghi chú hoàn tiền."));
        order.setRefundedBy(processedBy);
        RefundTransaction refundTransaction = refundPayoutService.payoutOrderRefund(order, processedBy, order.getRefundNote());
        applyOrderRefundResult(order, refundTransaction);
        orderRepository.save(order);
    }

    /** Chọn lại phương thức thanh toán cho đơn đang chờ và tạo giao dịch trực tuyến nếu cần. */
    @Override
    @Transactional
    public CheckoutResult choosePaymentMethod(Integer id, User user, String paymentMethodValue) {
        Order selectedOrder = getOrderForUser(id, user);
        Order rootOrder = selectedOrder.getParentOrder() != null ? selectedOrder.getParentOrder() : selectedOrder;
        if (!isRetryablePaymentOrder(rootOrder)) {
            throw new IllegalStateException("Chỉ có thể chọn lại thanh toán cho đơn chưa thanh toán hoặc đã hết hạn thanh toán.");
        }
        List<Order> stockOrders = rootOrder.getParentOrder() == null && rootOrder.getOrderType() != OrderType.PARENT
                ? List.of(rootOrder)
                : rootOrder.getSubOrders();
        PaymentMethod paymentMethod = parsePaymentMethod(paymentMethodValue);
        stockOrders.forEach(order -> {
            order.setPaymentMethod(paymentMethod);
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        });
        rootOrder.setPaymentMethod(paymentMethod);
        rootOrder.setPaymentStatus(PaymentStatus.PENDING);
        inventoryReservationService.reserveStock(stockOrders);
        Booking booking = stockOrders.stream()
                .filter(order -> order.getOrderType() == OrderType.AT_WORKSHOP)
                .map(order -> bookingRepository.findByRelatedOrder(order).orElse(null))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        PaymentTransaction transaction = createPayOSPayment(user,
                rootOrder.getOrderType() == OrderType.PARENT ? rootOrder : null,
                stockOrders,
                booking);
        return new CheckoutResult(rootOrder.getId(), transaction.getCheckoutUrl());
    }

    /** Cập nhật trạng thái đơn theo luồng xử lý, giao, hoàn tất, hủy hoặc hết hạn. */
    @Override
    @Transactional
    public void updateOrderStatus(Integer id, String status) {
        Order order = getOrderById(id);
        OrderStatus nextStatus = OrderStatus.valueOf(status.toUpperCase());
        if (order.getOrderStatus() == nextStatus) {
            return;
        }
        validateOrderTransition(order, nextStatus);
        if (nextStatus == OrderStatus.PROCESSING) {
            orderWorkflowService.processOrder(order);
        } else if (nextStatus == OrderStatus.SHIPPING) {
            requireText(order.getShippingCarrier(), "Vui lòng cập nhật đơn vị vận chuyển trước khi chuyển sang đang giao.");
            requireText(order.getTrackingCode(), "Vui lòng cập nhật mã vận đơn trước khi chuyển sang đang giao.");
            orderWorkflowService.shipOrder(order);
        } else if (nextStatus == OrderStatus.COMPLETED) {
            orderWorkflowService.completeOrder(order);
        } else if (nextStatus == OrderStatus.CANCELED || nextStatus == OrderStatus.EXPIRED_PAYMENT) {
            if (nextStatus == OrderStatus.EXPIRED_PAYMENT) {
                order.setOrderStatus(OrderStatus.EXPIRED_PAYMENT);
                inventoryReservationService.releaseReservation(order);
                orderRepository.save(order);
            } else {
                orderWorkflowService.cancelOrder(order);
            }
        } else {
            order.setOrderStatus(nextStatus);
            orderRepository.save(order);
        }
    }

    /** Tạo đơn hoặc đơn con, sao chép thông tin sản phẩm vào dòng đơn và kiểm tra tồn kho. */
    private Order createOrder(User user, OrderType orderType, Order parentOrder, ShippingDetails shippingDetails, String receiverPhone, List<CartItem> cartItems) {
        BigDecimal productTotal = calculateProductTotal(cartItems);
        Order order = new Order();
        order.setUser(user);
        order.setParentOrder(parentOrder);
        order.setOrderType(orderType);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setRefundStatus(RefundStatus.NONE);
        if (shippingDetails != null) {
            order.setShippingProvince(shippingDetails.province());
            order.setShippingDistrict(shippingDetails.district());
            order.setShippingWard(shippingDetails.ward());
            order.setShippingAddress(shippingDetails.address());
            order.setShippingFee(shippingDetails.fee());
        }
        order.setReceiverPhone(receiverPhone);
        order.setProductTotal(productTotal);
        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));
            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setProduct(product);
            item.setProductNameSnapshot(product.getProductName());
            item.setQuantity(cartItem.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            item.setFulfillmentType(cartItem.getFulfillmentType());
            savedOrder.getOrderItems().add(item);
            orderItemRepository.save(item);
        }
        return savedOrder;
    }

    /** Chuẩn hóa địa chỉ giao hàng và chụp lại mức phí giao tại thời điểm khách đặt đơn. */
    private ShippingDetails resolveShippingDetails(CheckoutForm form) {
        String province = requireText(form.getShippingProvince(), "Vui lòng chọn tỉnh/thành nhận hàng.");
        String district = requireText(form.getShippingDistrict(), "Vui lòng chọn quận/huyện nhận hàng.");
        String ward = requireText(form.getShippingWard(), "Vui lòng nhập phường/xã nhận hàng.");
        String address = requireText(form.getShippingAddress(), "Vui lòng nhập địa chỉ nhận hàng.");
        return new ShippingDetails(province, district, ward, address, shippingFeeService.resolveFee(province, district));
    }

    /** Cộng gộp số lượng theo sản phẩm và kiểm tra tồn kho lần cuối trước checkout. */
    private void validateCartStock(List<CartItem> cartItems) {
        Map<Integer, Integer> quantitiesByProductId = cartItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getId(),
                        Collectors.summingInt(CartItem::getQuantity)
                ));

        quantitiesByProductId.forEach((productId, totalQuantity) -> {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found."));
            inventoryReservationService.checkStockAvailability(product, totalQuantity);
        });
    }

    /** Bảo đảm giỏ chỉ chứa một kiểu nhận hàng theo chính sách tách đơn hiện tại. */
    private void validateSingleFulfillment(List<CartItem> cartItems) {
        long fulfillmentTypeCount = cartItems.stream()
                .map(CartItem::getFulfillmentType)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        if (fulfillmentTypeCount > 1) {
            throw new MixedFulfillmentException();
        }
    }

    /**
     * Tạo lịch lắp đặt liên kết với đơn phụ tùng; phí và khoản cọc được chụp lại
     * tại thời điểm đặt để không bị ảnh hưởng bởi thay đổi giá sau này.
     */
    /** Tạo lịch lắp đặt, chụp lại phí/cọc và liên kết lịch với đơn phụ tùng tại xưởng. */
    private Booking createWorkshopBooking(User user, CheckoutForm form, Order relatedOrder) {
        com.hsf302.carshowroom.entity.Service service = serviceRepository
                .findFirstByServiceNameIgnoreCase(WORKSHOP_INSTALLATION_SERVICE)
                .filter(candidate -> candidate.getStatus() == ServiceStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException(
                        "Dịch vụ 'Thay thế phụ tùng' chưa sẵn sàng. Vui lòng liên hệ quản trị viên."));
        Vehicle vehicle = vehicleRepository.findById(form.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe."));
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Xe này không thuộc tài khoản của bạn.");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setVehicle(vehicle);
        booking.setRelatedOrder(relatedOrder);
        booking.setBookingType(BookingType.PART_INSTALLATION);
        booking.setBookingDate(form.getBookingDate());
        booking.setStartTime(form.getStartTime());
        booking.setEndTime(form.getStartTime().plusMinutes(service.getDurationMinutes()));
        booking.setTimeSlot(formatSlot(booking.getStartTime(), booking.getEndTime()));
        booking.setTotalDurationMinutes(service.getDurationMinutes());
        booking.setEstimatedMinAmount(service.getMinPrice());
        booking.setEstimatedMaxAmount(service.getMaxPrice());
        booking.setFinalAmount(service.getMinPrice());
        booking.setDepositAmount(service.getMinPrice().multiply(BigDecimal.valueOf(
                        settingService.getInt(SystemSettingServiceImpl.DEPOSIT_RATE_PERCENT)))
                .movePointLeft(2)
                .max(BigDecimal.valueOf(settingService.getInt(SystemSettingServiceImpl.MIN_DEPOSIT_AMOUNT)))
                .min(BigDecimal.valueOf(settingService.getInt(SystemSettingServiceImpl.MAX_DEPOSIT_AMOUNT)))
                .setScale(0, RoundingMode.UP));
        booking.setBookingStatus(com.hsf302.carshowroom.common.Enums.BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setPaymentDeadline(LocalDateTime.now().plusMinutes(
                settingService.getInt(SystemSettingServiceImpl.PAYMENT_HOLD_MINUTES)));
        booking.setNotes(form.getNotes());
        schedulingService.holdSlot(booking);
        Booking savedBooking = bookingRepository.save(booking);

        com.hsf302.carshowroom.entity.BookingService snapshot = new com.hsf302.carshowroom.entity.BookingService();
        snapshot.setBooking(savedBooking);
        snapshot.setService(service);
        snapshot.setServiceNameSnapshot(service.getServiceName());
        snapshot.setDurationMinutesSnapshot(service.getDurationMinutes());
        snapshot.setMinPriceSnapshot(service.getMinPrice());
        snapshot.setMaxPriceSnapshot(service.getMaxPrice());
        bookingServiceRepository.save(snapshot);
        return savedBooking;
    }

    /** Kiểm tra các dữ liệu bắt buộc trước khi giữ lịch lắp đặt tại xưởng. */
    private void validateWorkshopCheckout(CheckoutForm form) {
        if (form.getVehicleId() == null || form.getBookingDate() == null || form.getStartTime() == null) {
            throw new RuntimeException("Vui lòng chọn xe, ngày hẹn và giờ hẹn để lắp đặt tại xưởng.");
        }
    }

    /** Tính tổng tiền phụ tùng từ các dòng giỏ hàng. */
    private BigDecimal calculateProductTotal(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Ghép giờ bắt đầu và giờ kết thúc thành chuỗi khung giờ hiển thị. */
    private String formatSlot(LocalTime startTime, LocalTime endTime) {
        return startTime + " - " + endTime;
    }

    /** Đồng bộ trạng thái hoàn tiền của đơn hàng theo kết quả payout từ cổng thanh toán. */
    private void applyOrderRefundResult(Order order, RefundTransaction refundTransaction) {
        RefundPayoutStatus payoutStatus = refundTransaction.getPayoutStatus();
        if (payoutStatus == RefundPayoutStatus.SUCCEEDED) {
            order.setRefundStatus(RefundStatus.COMPLETED);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            order.setRefundedAt(refundTransaction.getRefundedAt());
        } else if (payoutStatus == RefundPayoutStatus.FAILED) {
            order.setRefundStatus(RefundStatus.FAILED);
            order.setRefundNote(refundTransaction.getErrorMessage());
        } else {
            order.setRefundStatus(RefundStatus.PROCESSING);
        }
    }

    /** Dữ liệu giao hàng đã được kiểm tra và cố định phí khi checkout. */
    private record ShippingDetails(String province, String district, String ward, String address, BigDecimal fee) {
    }

    /** Tạo giao dịch PayOS; khi có Booking, PayOS chỉ thu khoản cọc của lịch hẹn. */
    private PaymentTransaction createPayOSPayment(User user, Order parentOrder, List<Order> stockOrders, Booking booking) {
        return paymentService.createPaymentLink(PayOSCreatePaymentLinkRequest.builder()
                .user(user)
                .parentOrder(parentOrder)
                .subOrders(stockOrders)
                .booking(booking)
                .build());
    }

    /** Chuyển chuỗi phương thức thanh toán thành enum hợp lệ. */
    private PaymentMethod parsePaymentMethod(String value) {
        if (value == null || !PaymentMethod.PAYOS.name().equalsIgnoreCase(value.trim())) {
            throw new IllegalArgumentException("Chỉ hỗ trợ thanh toán trực tuyến qua PayOS.");
        }
        return PaymentMethod.PAYOS;
    }

    /** Kiểm tra quy tắc chuyển trạng thái của đơn hàng trước khi thực hiện thao tác. */
    private void validateOrderTransition(Order order, OrderStatus nextStatus) {
        OrderStatus currentStatus = order.getOrderStatus();
        if (nextStatus == OrderStatus.CANCELED) {
            if (currentStatus == OrderStatus.COMPLETED
                    || currentStatus == OrderStatus.CANCELED
                    || currentStatus == OrderStatus.EXPIRED_PAYMENT
                    || currentStatus == OrderStatus.SHIPPING) {
                throw new IllegalStateException("Không thể hủy đơn ở trạng thái hiện tại.");
            }
            return;
        }
        if (nextStatus == OrderStatus.EXPIRED_PAYMENT) {
            if (currentStatus != OrderStatus.PENDING_PAYMENT && currentStatus != OrderStatus.CREATED) {
                throw new IllegalStateException("Chỉ đơn đang chờ thanh toán mới có thể hết hạn.");
            }
            return;
        }
        if (nextStatus == OrderStatus.PROCESSING) {
            if (currentStatus != OrderStatus.PENDING_PAYMENT && currentStatus != OrderStatus.CREATED) {
                throw new IllegalStateException("Chỉ đơn mới hoặc đang chờ thanh toán mới có thể chuyển sang đang xử lý.");
            }
            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                throw new IllegalStateException("Đơn PayOS chưa được xác nhận thanh toán.");
            }
            return;
        }
        if (nextStatus == OrderStatus.SHIPPING) {
            if (currentStatus != OrderStatus.PROCESSING || order.getOrderType() != OrderType.SHIPPING) {
                throw new IllegalStateException("Chỉ đơn giao hàng đang xử lý mới có thể chuyển sang đang giao.");
            }
            return;
        }
        if (nextStatus == OrderStatus.COMPLETED) {
            boolean shippingCompleted = order.getOrderType() == OrderType.SHIPPING
                    && currentStatus == OrderStatus.SHIPPING;
            boolean workshopCompleted = order.getOrderType() == OrderType.AT_WORKSHOP
                    && currentStatus == OrderStatus.PROCESSING;
            if (!shippingCompleted && !workshopCompleted) {
                throw new IllegalStateException("Đơn hàng chưa đạt trạng thái có thể hoàn tất.");
            }
            return;
        }
        throw new IllegalStateException("Trạng thái đơn hàng không được phép cập nhật thủ công.");
    }

    /** Hủy đơn cha và toàn bộ đơn con, dùng chung lý do hủy. */
    private void cancelOrderAndChildren(Order order) {
        for (Order subOrder : order.getSubOrders()) {
            subOrder.setCancellationReason(order.getCancellationReason());
            orderWorkflowService.cancelOrder(subOrder);
        }
        orderWorkflowService.cancelOrder(order);
    }

    /** Kiểm tra đơn hàng thuộc khách hàng đang thao tác. */
    private void assertOrderOwner(Order order, User user) {
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Bạn không có quyền thao tác đơn hàng này.");
        }
    }

    /** Bắt buộc trường văn bản có giá trị và trả về giá trị đã bỏ khoảng trắng thừa. */
    private String requireText(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    /** Xác định khách còn được phép hủy đơn theo trạng thái hiện tại hay không. */
    private boolean isCustomerCancelable(Order order) {
        return order.getOrderStatus() != OrderStatus.SHIPPING
                && order.getOrderStatus() != OrderStatus.COMPLETED
                && order.getOrderStatus() != OrderStatus.CANCELED
                && order.getOrderStatus() != OrderStatus.EXPIRED_PAYMENT;
    }

    private boolean isRetryablePaymentOrder(Order order) {
        return order.getPaymentStatus() != PaymentStatus.PAID
                && (order.getOrderStatus() == OrderStatus.PENDING_PAYMENT
                || order.getOrderStatus() == OrderStatus.EXPIRED_PAYMENT);
    }
}
