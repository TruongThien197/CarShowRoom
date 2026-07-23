package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.FulfillmentType;
import com.hsf302.carshowroom.common.Enums.OrderStatus;
import com.hsf302.carshowroom.common.Enums.OrderType;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.common.Enums.PaymentMethod;
import com.hsf302.carshowroom.common.Enums.RefundPayoutStatus;
import com.hsf302.carshowroom.common.Enums.ServiceStatus;
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
import com.hsf302.carshowroom.repository.BookingServiceRepository;
import com.hsf302.carshowroom.repository.CartItemRepository;
import com.hsf302.carshowroom.repository.OrderItemRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.repository.PaymentTransactionRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.InventoryReservationService;
import com.hsf302.carshowroom.service.OrderService;
import com.hsf302.carshowroom.service.OrderWorkflowService;
import com.hsf302.carshowroom.service.PaymentService;
import com.hsf302.carshowroom.service.RefundPayoutService;
import com.hsf302.carshowroom.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final InventoryReservationService inventoryReservationService;
    private final SchedulingService schedulingService;
    private final PaymentService paymentService;
    private final OrderWorkflowService orderWorkflowService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundPayoutService refundPayoutService;

    @Override
    @Transactional
    public CheckoutResult checkout(User user, CheckoutForm form) {
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống.");
        }

        Map<FulfillmentType, List<CartItem>> groupedItems = cartItems.stream()
                .collect(Collectors.groupingBy(item -> item.getFulfillmentType() == null
                        ? FulfillmentType.SHIPPING
                        : item.getFulfillmentType()));

        List<CartItem> shippingItems = groupedItems.getOrDefault(FulfillmentType.SHIPPING, List.of());
        List<CartItem> workshopItems = groupedItems.getOrDefault(FulfillmentType.AT_WORKSHOP, List.of());
        boolean mixed = !shippingItems.isEmpty() && !workshopItems.isEmpty();
        PaymentMethod paymentMethod = parsePaymentMethod(form.getPaymentMethod());

        Order parentOrder = mixed ? createOrder(user, OrderType.PARENT, null, form.getShippingAddress(), form.getPhone(), List.of()) : null;
        List<Order> stockOrders = new ArrayList<>();
        Booking booking = null;

        if (!shippingItems.isEmpty()) {
            stockOrders.add(createOrder(user, OrderType.SHIPPING, parentOrder, form.getShippingAddress(), form.getPhone(), shippingItems));
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
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setRefundStatus(RefundStatus.REQUESTED);
        }
        cancelOrderAndChildren(order);
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
                || (order.getRefundStatus() != RefundStatus.REQUESTED && order.getRefundStatus() != RefundStatus.FAILED)
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

    private Order createOrder(User user, OrderType orderType, Order parentOrder, String shippingAddress, String receiverPhone, List<CartItem> cartItems) {
        BigDecimal productTotal = calculateProductTotal(cartItems);
        Order order = new Order();
        order.setUser(user);
        order.setParentOrder(parentOrder);
        order.setOrderType(orderType);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setRefundStatus(RefundStatus.NONE);
        order.setShippingAddress(shippingAddress);
        order.setReceiverPhone(receiverPhone);
        order.setProductTotal(productTotal);
        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm."));
            inventoryReservationService.checkStockAvailability(product, cartItem.getQuantity());
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
        com.hsf302.carshowroom.entity.Service service = serviceRepository.findById(form.getServiceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ."));
        if (service.getStatus() != ServiceStatus.ACTIVE) {
            throw new RuntimeException("Dịch vụ này hiện không khả dụng.");
        }
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
        booking.setEndTime(form.getStartTime().plusMinutes(service.getDurationMinutes()));
        booking.setTimeSlot(formatSlot(booking.getStartTime(), booking.getEndTime()));
        booking.setTotalDurationMinutes(service.getDurationMinutes());
        booking.setEstimatedMinAmount(service.getMinPrice());
        booking.setEstimatedMaxAmount(service.getMaxPrice());
        booking.setFinalAmount(service.getMinPrice());
        booking.setDepositAmount(service.getMinPrice().multiply(BigDecimal.valueOf(0.20))
                .max(BigDecimal.valueOf(2_000))
                .min(BigDecimal.valueOf(10_000))
                .setScale(0, RoundingMode.UP));
        booking.setBookingStatus(com.hsf302.carshowroom.common.Enums.BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
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

    private void validateWorkshopCheckout(CheckoutForm form) {
        if (form.getVehicleId() == null || form.getServiceId() == null || form.getBookingDate() == null || form.getStartTime() == null) {
            throw new RuntimeException("Vui lòng chọn xe, dịch vụ, ngày và giờ hẹn cho sản phẩm lắp đặt tại xưởng.");
        }
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
