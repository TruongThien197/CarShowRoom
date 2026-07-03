package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.dto.CheckoutForm;
import com.hsf302.carshowroom.entity.CartItem;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.OrderDetail;
import com.hsf302.carshowroom.entity.Product;
import com.hsf302.carshowroom.entity.User;
import com.hsf302.carshowroom.repository.CartItemRepository;
import com.hsf302.carshowroom.repository.OrderDetailRepository;
import com.hsf302.carshowroom.repository.OrderRepository;
import com.hsf302.carshowroom.repository.ProductRepository;
import com.hsf302.carshowroom.service.CartService;
import com.hsf302.carshowroom.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    @Override
    @Transactional
    public Order checkout(User user, CheckoutForm form) {
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Giỏ hàng của bạn đang trống.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(Instant.now());
        order.setShippingAddress(form.getShippingAddress());
        order.setStatus("PENDING");
        order.setTotalAmount(cartService.calculateSubtotal(cartItems));
        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException(
                        "Sản phẩm " + product.getProductName() + " không đủ số lượng trong kho."); }
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            OrderDetail detail = new OrderDetail();
            detail.setOrder(savedOrder);
            detail.setProduct(product);
            detail.setQuantity(cartItem.getQuantity());
            detail.setUnitPrice(product.getPrice());
            orderDetailRepository.save(detail);
        }

        cartItemRepository.deleteByUser(user);
        return savedOrder;
    }


    @Override
    public List<Order> getOrders(User user) {
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }


    @Override
    public Order getOrderById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với id: " + id));
    }

    @Override
    @Transactional
    public void updateOrderStatus(Integer id, String status) {
        Order order = getOrderById(id);
        String upperStatus = status.toUpperCase();
        order.setStatus(upperStatus);
        orderRepository.save(order);
    }
}
