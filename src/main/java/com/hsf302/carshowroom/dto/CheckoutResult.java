package com.hsf302.carshowroom.dto;

public record CheckoutResult(Integer orderId, String checkoutUrl) {
    public boolean requiresOnlinePayment() {
        return checkoutUrl != null && !checkoutUrl.isBlank();
    }
}
