package com.hsf302.carshowroom.service;

import com.hsf302.carshowroom.entity.Booking;
import com.hsf302.carshowroom.entity.Order;
import com.hsf302.carshowroom.entity.RefundTransaction;
import com.hsf302.carshowroom.entity.User;

public interface RefundPayoutService {
    RefundTransaction payoutOrderRefund(Order order, User processedBy, String note);

    RefundTransaction payoutBookingRefund(Booking booking, User processedBy, String bankName,
                                          String bankBin, String accountHolder,
                                          String accountNumber, String note);

    RefundTransaction syncRefundTransaction(Long refundTransactionId);
}
