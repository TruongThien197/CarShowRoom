package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.FulfillmentType;
import com.hsf302.carshowroom.common.Enums.OrderStatus;
import com.hsf302.carshowroom.common.Enums.OrderType;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.common.Enums.ServiceStatus;
import com.hsf302.carshowroom.dto.CheckoutForm;
import com.hsf302.carshowroom.dto.PayOS.PayOSCreatePaymentLinkRequest;
import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.OrderItem;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.entity.Vehicle;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.BookingServiceRepository;
import com.hsf302.carshowroom.repository.CartItemRepository;
import com.hsf302.carshowroom.repository.OrderItemRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.InventoryReservationService;
import com.hsf302.carshowroom.service.OrderService;
import com.hsf302.carshowroom.service.OrderWorkflowService;
import com.hsf302.carshowroom.service.PaymentService;
import com.hsf302.carshowroom.service.SchedulingService;
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
    private final ServiceRepository serviceRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final InventoryReservationService inventoryReservationService;
    private final SchedulingService schedulingService;
    private final PaymentService paymentService;
    private final OrderWorkflowService orderWorkflowService;

    @Override
    @Transactional
    public PaymentTransaction checkout(User user, CheckoutForm form) {
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Your cart is empty.");
        }

        Map<FulfillmentType, List<CartItem>> groupedItems = cartItems.stream()
                .collect(Collectors.groupingBy(item -> item.getFulfillmentType() == null
                        ? FulfillmentType.SHIPPING
                        : item.getFulfillmentType()));

        List<CartItem> shippingItems = groupedItems.getOrDefault(FulfillmentType.SHIPPING, List.of());
        List<CartItem> workshopItems = groupedItems.getOrDefault(FulfillmentType.AT_WORKSHOP, List.of());
        boolean mixed = !shippingItems.isEmpty() && !workshopItems.isEmpty();

        Order parentOrder = mixed ? createOrder(user, OrderType.PARENT, null, form.getShippingAddress(), List.of()) : null;
        List<Order> stockOrders = new ArrayList<>();
        Booking booking = null;

        if (!shippingItems.isEmpty()) {
            stockOrders.add(createOrder(user, OrderType.SHIPPING, parentOrder, form.getShippingAddress(), shippingItems));
        }
        if (!workshopItems.isEmpty()) {
            validateWorkshopCheckout(form);
            Order workshopOrder = createOrder(user, OrderType.AT_WORKSHOP, parentOrder, null, workshopItems);
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

        inventoryReservationService.reserveStock(stockOrders);
        PaymentTransaction transaction = paymentService.createPaymentLink(PayOSCreatePaymentLinkRequest.builder()
                .user(user)
                .parentOrder(parentOrder)
                .subOrders(stockOrders)
                .booking(booking)
                .build());
        cartItemRepository.deleteByUser(user);

        return transaction;
    }

    @Override
    public List<Order> getOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Override
    public Order getOrderById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    @Override
    @Transactional
    public void updateOrderStatus(Integer id, String status) {
        Order order = getOrderById(id);
        OrderStatus nextStatus = OrderStatus.valueOf(status.toUpperCase());
        if (nextStatus == OrderStatus.PROCESSING) {
            order.setPaymentStatus(PaymentStatus.PAID);
            orderWorkflowService.processOrder(order);
        } else if (nextStatus == OrderStatus.SHIPPING) {
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

    private Order createOrder(User user, OrderType orderType, Order parentOrder, String shippingAddress, List<CartItem> cartItems) {
        BigDecimal productTotal = calculateProductTotal(cartItems);
        Order order = new Order();
        order.setUser(user);
        order.setParentOrder(parentOrder);
        order.setOrderType(orderType);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setShippingAddress(shippingAddress);
        order.setProductTotal(productTotal);
        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found."));
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
                .orElseThrow(() -> new RuntimeException("Service not found."));
        if (service.getStatus() != ServiceStatus.ACTIVE) {
            throw new RuntimeException("This service is currently unavailable.");
        }
        Vehicle vehicle = vehicleRepository.findById(form.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found."));
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("This vehicle does not belong to your account.");
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
            throw new RuntimeException("Please select a vehicle, service, date, and appointment time for workshop installation items.");
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
}
