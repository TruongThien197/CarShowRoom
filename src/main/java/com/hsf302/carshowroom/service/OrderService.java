package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.CheckoutForm;
import com.hsf302.carshowroom.dto.CheckoutResult;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import com.hsf302.carshowroom.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {
    CheckoutResult checkout(User user, CheckoutForm form);

    List<Order> getOrders(User user);

    Order getOrderById(Integer id);

    void updateOrderStatus(Integer id, String status);

    Order getOrderForUser(Integer id, User user);

    void cancelOrderForUser(Integer id, User user, String reason);
    void approveCancellation(Integer id, User processedBy);
    void rejectCancellation(Integer id, User processedBy, String reason);

    void updateShippingAddressForUser(Integer id, User user, String shippingAddress, String receiverPhone);

    void updateShipment(Integer id, String shippingCarrier, String trackingCode);
    /** Xác nhận nhân viên đã chuyển khoản hoàn tiền thủ công và lưu mã giao dịch. */
    void completeRefund(Integer id, User processedBy, String transactionCode);

    /** Lưu tài khoản khách nhận tiền sau khi yêu cầu hủy đơn đã được duyệt. */
    void submitRefundAccount(User user, Integer id, String bankName,
                             String accountHolder, String accountNumber);

    CheckoutResult choosePaymentMethod(Integer id, User user, String paymentMethod);
    BigDecimal calculateTotalAmount(Order order);
}
