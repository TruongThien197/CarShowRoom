package com.hsf302.carshowroom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {
    private final PaymentService paymentService;

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void expirePendingPayments() {
        paymentService.expirePendingPayments();
    }
}
