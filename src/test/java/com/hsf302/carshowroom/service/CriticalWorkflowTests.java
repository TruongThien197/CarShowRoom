package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.common.Enums.OrderStatus;
import com.hsf302.carshowroom.common.Enums.OrderType;
import com.hsf302.carshowroom.common.Enums.PaymentMethod;
import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.BookingRepository;
import com.hsf302.carshowroom.repository.BookingServiceRepository;
import com.hsf302.carshowroom.repository.CartItemRepository;
import com.hsf302.carshowroom.repository.OrderItemRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.repository.ServiceRepository;
import com.hsf302.carshowroom.repository.UserRepository;
import com.hsf302.carshowroom.repository.VehicleRepository;
import com.hsf302.carshowroom.service.impl.AuthServiceImpl;
import com.hsf302.carshowroom.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Mock private ProductRepository productRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingServiceRepository bookingServiceRepository;
    @Mock private InventoryReservationService inventoryReservationService;
    @Mock private SchedulingService schedulingService;
    @Mock private PaymentService paymentService;
    @Mock private OrderWorkflowService orderWorkflowService;
    @InjectMocks private OrderServiceImpl orderService;

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
}
