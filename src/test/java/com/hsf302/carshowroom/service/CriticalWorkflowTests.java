package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.common.Enums.OrderStatus;
import com.hsf302.carshowroom.common.Enums.OrderType;
import com.hsf302.carshowroom.common.Enums.PaymentMethod;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.common.Enums.ProductStatus;
import com.hsf302.carshowroom.dto.CheckoutResult;
import com.hsf302.carshowroom.entity.InventoryReservation;
import com.hsf302.carshowroom.entity.OrderItem;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.BookingServiceRepository;
import com.hsf302.carshowroom.repository.CartItemRepository;
import com.hsf302.carshowroom.repository.InventoryReservationRepository;
import com.hsf302.carshowroom.repository.OrderItemRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.UserRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.impl.AuthServiceImpl;
import com.hsf302.carshowroom.service.impl.CartServiceImpl;
import com.hsf302.carshowroom.service.impl.InventoryReservationServiceImpl;
import com.hsf302.carshowroom.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriticalWorkflowTests {
    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @InjectMocks private AuthServiceImpl authService;

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private InventoryReservationRepository reservationRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingServiceRepository bookingServiceRepository;
    @Mock private InventoryReservationService inventoryReservationService;
    @Mock private SchedulingService schedulingService;
    @Mock private PaymentService paymentService;
    @Mock private OrderWorkflowService orderWorkflowService;
    @Mock private RefundPayoutService refundPayoutService;
    @InjectMocks private OrderServiceImpl orderService;
    @InjectMocks private CartServiceImpl cartService;
    @InjectMocks private InventoryReservationServiceImpl inventoryReservationServiceImpl;

    @Test
    void inactiveAccountCannotLogin() {
        User user = User.builder()
                .email("locked@example.com")
                .passwordHash("hash")
                .status("INACTIVE")
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.login(user.getEmail(), "123456"));
    }

    @Test
    void unpaidPayOSOrderCannotBeMarkedProcessing() {
        Order order = new Order();
        order.setId(10);
        order.setOrderType(OrderType.SHIPPING);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentMethod(PaymentMethod.PAYOS);
        order.setPaymentStatus(PaymentStatus.PENDING);
        when(orderRepository.findById(10)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class,
                () -> orderService.updateOrderStatus(10, "PROCESSING"));
        verify(orderWorkflowService, never()).processOrder(order);
    }

    @Test
    void cartRejectsQuantityGreaterThanAvailableStock() {
        User user = new User();
        user.setId(1);
        Product product = new Product();
        product.setId(5);
        product.setName("Brake Pad");
        product.setPhysicalStock(2);
        product.setReservedStock(0);
        product.setPrice(BigDecimal.valueOf(1000));
        product.setStatus(ProductStatus.ACTIVE);
        when(productRepository.findById(5)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () -> cartService.addToCart(user, 5, 3, "SHIPPING"));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void secondReservationFailsWhenTwoOrdersCompeteForSameLastItem() {
        Product product = new Product();
        product.setId(8);
        product.setName("Turbo Kit");
        product.setPhysicalStock(1);
        product.setReservedStock(0);
        product.setStatus(ProductStatus.ACTIVE);

        Order firstOrder = orderWithSingleItem(product, 1, 101);
        Order secondOrder = orderWithSingleItem(product, 1, 102);
        when(productRepository.findByIdForUpdate(8))
                .thenReturn(Optional.of(product))
                .thenReturn(Optional.of(product));

        inventoryReservationServiceImpl.reserveStock(List.of(firstOrder));

        assertThrows(RuntimeException.class,
                () -> inventoryReservationServiceImpl.reserveStock(List.of(secondOrder)));
        verify(productRepository, atLeastOnce()).save(product);
        verify(reservationRepository, atLeastOnce()).save(any(InventoryReservation.class));
    }

    @Test
    void expiredUnpaidOrderCanChoosePaymentAgain() {
        User user = new User();
        user.setId(1);
        Order order = new Order();
        order.setId(44);
        order.setUser(user);
        order.setOrderType(OrderType.SHIPPING);
        order.setOrderStatus(OrderStatus.EXPIRED_PAYMENT);
        order.setPaymentStatus(PaymentStatus.EXPIRED);
        order.setProductTotal(BigDecimal.valueOf(5000));
        when(orderRepository.findById(44)).thenReturn(Optional.of(order));

        CheckoutResult result = orderService.choosePaymentMethod(44, user, "COD");

        assertNull(result.checkoutUrl());
        verify(inventoryReservationService).reserveStock(List.of(order));
        verify(orderWorkflowService).processOrder(order);
    }

    private Order orderWithSingleItem(Product product, int quantity, int orderId) {
        Order order = new Order();
        order.setId(orderId);
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        order.getOrderItems().add(item);
        return order;
    }
}
