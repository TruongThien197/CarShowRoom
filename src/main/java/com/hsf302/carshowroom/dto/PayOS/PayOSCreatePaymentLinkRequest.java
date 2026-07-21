package com.hsf302.carshowroom.dto.PayOS;

import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.User;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PayOSCreatePaymentLinkRequest {
    private User user;
    private Order parentOrder;
    private List<Order> subOrders;
    private Booking booking;
}
