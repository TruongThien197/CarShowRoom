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
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.CartItemRepository;
import com.hsf302.carshowroom.repository.OrderItemRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.repository.PaymentTransactionRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.CartInventoryValidationService;
import com.hsf302.carshowroom.service.InventoryReservationService;
import com.hsf302.carshowroom.service.OrderService;
import com.hsf302.carshowroom.service.OrderWorkflowService;
import com.hsf302.carshowroom.service.PaymentService;
import com.hsf302.carshowroom.service.RefundPayoutService;
import com.hsf302.carshowroom.service.SchedulingService;
import com.hsf302.carshowroom.service.ShippingFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final InventoryReservationService inventoryReservationService;
    private final CartInventoryValidationService cartInventoryValidationService;
    private final SchedulingService schedulingService;
    private final PaymentService paymentService;
    private final OrderWorkflowService orderWorkflowService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundPayoutService refundPayoutService;
    private final ShippingFeeService shippingFeeService;

    private static final int INSTALLATION_DURATION_MINUTES = 120;
    private static final List<LocalTime> INSTALLATION_SLOT_STARTS = List.of(
            LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(13, 0), LocalTime.of(15, 0));

    @Override
    @Transactional
    public CheckoutResult checkout(User user, CheckoutForm form) {
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
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

    @Override
    public List<Order> getOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public Order getOrderById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));
    }

    @Override
    public Order getOrderForUser(Integer id, User user) {
        Order order = getOrderById(id);
        assertOrderOwner(order, user);
        return order;
    }

    @Override
    @Transactional
    public void cancelOrderForUser(Integer id, User user, String reason) {
        Order order = getOrderForUser(id, user);
        if (!isCustomerCancelable(order)) {
            throw new IllegalStateException("Không thể hủy đơn khi đơn đã được giao, hoàn tất hoặc đã hủy.");
        }
        order.setCancellationReason(requireText(reason, "Vui lòng nhập lý do hủy đơn."));
        if (order.getRefundStatus() == RefundStatus.REQUESTED) {
            throw new IllegalStateException("Yêu cầu hủy đơn đã tồn tại và đang chờ duyệt.");
        }
        order.setRefundStatus(RefundStatus.REQUESTED);
        order.setCancellationRequestedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void approveCancellation(Integer id, User processedBy) {
        Order order = getOrderById(id);
        if (order.getRefundStatus() != RefundStatus.REQUESTED) {
            throw new IllegalStateException("Đơn hàng không có yêu cầu hủy đang chờ duyệt.");
        }
        cancelOrderAndChildren(order);
        order.setRefundStatus(order.getPaymentStatus() == PaymentStatus.PAID ? RefundStatus.APPROVED : RefundStatus.NONE);
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

    private void applyOrderRefundResult(Order order, RefundTransaction refundTransaction) {
        if (refundTransaction.getStatus() == RefundPayoutStatus.SUCCEEDED) {
            order.setRefundStatus(RefundStatus.COMPLETED);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            order.setRefundedAt(refundTransaction.getRefundedAt() == null ? LocalDateTime.now() : refundTransaction.getRefundedAt());
            paymentTransactionRepository.findByOrderOrParentOrder(order, order).forEach(transaction -> {
                transaction.setStatus(PaymentStatus.REFUNDED);
                paymentTransactionRepository.save(transaction);
            });
        } else if (refundTransaction.getStatus() == RefundPayoutStatus.FAILED) {
            order.setRefundStatus(RefundStatus.FAILED);
            order.setRefundNote(refundTransaction.getErrorMessage());
        } else {
            order.setRefundStatus(RefundStatus.PROCESSING);
        }
    }

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

    @Override
    public BigDecimal calculateTotalAmount(Order order) {
        System.out.println("ORDER ID: " + order.getId());
        System.out.println("PRODUCT: " + order.getProductTotal());
        System.out.println("SHIP: " + order.getShippingFee());
        BigDecimal productTotal = order.getProductTotal() != null
                ? order.getProductTotal()
                : BigDecimal.ZERO;

        BigDecimal shippingFee = order.getShippingFee() != null
                ? order.getShippingFee()
                : BigDecimal.ZERO;

        return productTotal.add(shippingFee);
    }

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

    private Booking createWorkshopBooking(User user, CheckoutForm form, Order relatedOrder) {
        Vehicle vehicle = vehicleRepository.findById(form.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe."));
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Xe này không thuộc tài khoản của bạn.");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setVehicle(vehicle);
        booking.setRelatedOrder(relatedOrder);
        booking.setBookingDate(form.getBookingDate());
        booking.setStartTime(form.getStartTime());
        booking.setEndTime(form.getStartTime().plusMinutes(INSTALLATION_DURATION_MINUTES));
        booking.setTimeSlot(formatSlot(booking.getStartTime(), booking.getEndTime()));
        booking.setTotalDurationMinutes(INSTALLATION_DURATION_MINUTES);
        booking.setEstimatedMinAmount(BigDecimal.ZERO);
        booking.setEstimatedMaxAmount(BigDecimal.ZERO);
        booking.setFinalAmount(null);
        booking.setDepositAmount(BigDecimal.ZERO);
        booking.setBookingType(BookingType.PART_INSTALLATION);
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
        booking.setNotes(form.getNotes());
        schedulingService.holdSlot(booking);
        return bookingRepository.save(booking);
    }

    private void validateWorkshopCheckout(CheckoutForm form) {
        if (form.getVehicleId() == null || form.getBookingDate() == null || form.getStartTime() == null) {
            throw new RuntimeException("Vui lòng chọn xe, ngày và giờ hẹn cho sản phẩm lắp đặt tại xưởng.");
        }
        if (!INSTALLATION_SLOT_STARTS.contains(form.getStartTime())) {
            throw new RuntimeException("Vui lòng chọn một trong các khung giờ lắp đặt cố định.");
        }
    }

    private ShippingDetails resolveShippingDetails(CheckoutForm form) {
        String province = requireText(form.getShippingProvince(), "Vui lòng chọn tỉnh/thành phố giao hàng.");
        String district = requireText(form.getShippingDistrict(), "Vui lòng chọn quận/huyện giao hàng.");
        String ward = requireText(form.getShippingWard(), "Vui lòng nhập phường/xã giao hàng.");
        String address = requireText(form.getShippingAddress(), "Vui lòng nhập địa chỉ giao hàng chi tiết.");
        return new ShippingDetails(province, district, ward, address, shippingFeeService.resolveFee(province, district));
    }

    private record ShippingDetails(String province, String district, String ward, String address, BigDecimal fee) {
    }

    private BigDecimal calculateProductTotal(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatSlot(LocalTime startTime, LocalTime endTime) {
        return startTime + " - " + endTime;
    }

    private PaymentTransaction createPayOSPayment(User user, Order parentOrder, List<Order> stockOrders, Booking booking) {
        return paymentService.createPaymentLink(PayOSCreatePaymentLinkRequest.builder()
                .user(user)
                .parentOrder(parentOrder)
                .subOrders(stockOrders)
                .booking(booking)
                .build());
    }

    private PaymentMethod parsePaymentMethod(String value) {
        if (value == null || !PaymentMethod.PAYOS.name().equalsIgnoreCase(value.trim())) {
            throw new IllegalArgumentException("Chỉ hỗ trợ thanh toán trực tuyến qua PayOS.");
        }
        return PaymentMethod.PAYOS;
    }

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

    private void cancelOrderAndChildren(Order order) {
        for (Order subOrder : order.getSubOrders()) {
            subOrder.setCancellationReason(order.getCancellationReason());
            orderWorkflowService.cancelOrder(subOrder);
        }
        orderWorkflowService.cancelOrder(order);
    }

    private void assertOrderOwner(Order order, User user) {
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Bạn không có quyền thao tác đơn hàng này.");
        }
    }

    private String requireText(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

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
