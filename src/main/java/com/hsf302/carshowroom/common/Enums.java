package com.hsf302.carshowroom.common;

public class Enums {

    public enum Role {
        CUSTOMER,
        STAFF,
        ADMIN
    }

    public enum ProductStatus {
        ACTIVE,
        INACTIVE
    }

    public enum ServiceStatus {
        ACTIVE,
        INACTIVE
    }

    public enum FulfillmentType {
        SHIPPING,
        AT_WORKSHOP
    }

    public enum OrderType {
        PARENT,
        SHIPPING,
        AT_WORKSHOP
    }

    public enum OrderStatus {
        CREATED,
        PENDING_PAYMENT,
        PROCESSING,
        SHIPPING,
        COMPLETED,
        CANCELED,
        EXPIRED_PAYMENT
    }

    public enum BookingStatus {
        CREATED,
        PENDING_PAYMENT,
        CONFIRMED,
        IN_PROGRESS,
        PENDING_APPROVAL,
        COMPLETED,
        CANCELED,
        EXPIRED_PAYMENT,
        EXPIRED_NO_SHOW
    }

    public enum PaymentStatus {
        INITIATED,
        PENDING,
        PAID,
        FAILED,
        CANCELED,
        EXPIRED,
        REFUNDED
    }

    public enum ReservationStatus {
        HELD,
        CONFIRMED,
        RELEASED,
        CONSUMED,
        EXPIRED
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
