package com.hsf302.carshowroom.entity;

import com.hsf302.carshowroom.common.Enums.BookingType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OrderFulfillmentFoundationTest {

    @Test
    void newProductDoesNotSupportWorkshopInstallationByDefault() {
        Product product = new Product();

        assertFalse(product.isInstallationSupported());
    }

    @Test
    void newBookingDefaultsToRepairServiceAndUncollectedLabor() {
        Booking booking = new Booking();

        assertEquals(BookingType.REPAIR_SERVICE, booking.getBookingType());
        assertFalse(booking.isLaborCollected());
    }

    @Test
    void newOrderStartsWithNoShippingFee() {
        Order order = new Order();

        assertEquals(BigDecimal.ZERO, order.getShippingFee());
    }
}
