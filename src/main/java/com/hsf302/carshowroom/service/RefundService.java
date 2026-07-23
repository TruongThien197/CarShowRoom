package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.RefundTransaction;
import com.hsf302.carshowroom.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface RefundService {
    RefundTransaction requestOrderRefund(Order order, String reason);
    RefundTransaction requestBookingRefund(Booking booking, BigDecimal amount, String reason);
    void complete(Long refundId, User processedBy, String note);
    List<RefundTransaction> getOrderRefunds(Order order);
    List<RefundTransaction> getBookingRefunds(Booking booking);
}
