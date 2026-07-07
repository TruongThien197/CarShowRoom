package com.hsf302.carshowroom.service.impl;

import com.hsf302.carshowroom.common.Enums.PaymentStatus;
import com.hsf302.carshowroom.dto.PayOS.PayOSCreatePaymentLinkRequest;
import com.hsf302.carshowroom.dto.PayOS.PayOSWebhookRequest;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.PaymentTransaction;
import com.hsf302.carshowroom.repository.PaymentTransactionRepository;
import com.hsf302.carshowroom.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    @Transactional
    public PaymentTransaction createPaymentLink(PayOSCreatePaymentLinkRequest request) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUser(request.getUser());
        transaction.setParentOrder(request.getParentOrder());
        transaction.setOrder(resolveMainOrder(request));
        transaction.setBooking(request.getBooking());
        transaction.setAmount(resolveDepositAmount(request));
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setPayosOrderCode("LOCAL-" + System.currentTimeMillis());
        transaction.setCheckoutUrl("/orders");
        transaction.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
        return paymentTransactionRepository.save(transaction);
    }

    @Override
    public void handlePayOSWebhook(PayOSWebhookRequest webhookRequest) {
        throw new UnsupportedOperationException("PayOS API/webhook will be configured after credentials are provided.");
    }

    private Order resolveMainOrder(PayOSCreatePaymentLinkRequest request) {
        if (request.getParentOrder() != null) {
            return null;
        }
        List<Order> subOrders = request.getSubOrders();
        return subOrders == null || subOrders.isEmpty() ? null : subOrders.get(0);
    }

    private BigDecimal resolveDepositAmount(PayOSCreatePaymentLinkRequest request) {
        BigDecimal amount = BigDecimal.ZERO;
        if (request.getParentOrder() != null) {
            amount = amount.add(request.getParentOrder().getDepositAmount());
        }
        if (request.getSubOrders() != null) {
            amount = amount.add(request.getSubOrders().stream()
                    .map(Order::getDepositAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        if (request.getBooking() != null) {
            amount = amount.add(request.getBooking().getDepositAmount());
        }
        return amount;
    }
}
