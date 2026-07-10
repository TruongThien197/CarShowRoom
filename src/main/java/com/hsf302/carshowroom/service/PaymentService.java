package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.dto.PayOS.PayOSCreatePaymentLinkRequest;
import com.hsf302.carshowroom.dto.PayOS.PayOSWebhookRequest;
import com.hsf302.carshowroom.entity.PaymentTransaction;

public interface PaymentService {
    PaymentTransaction createPaymentLink(PayOSCreatePaymentLinkRequest request);
    PaymentTransaction syncPaymentStatus(String orderCode);
    void handlePayOSWebhook(PayOSWebhookRequest webhookRequest);
    void expirePendingPayments();
}
