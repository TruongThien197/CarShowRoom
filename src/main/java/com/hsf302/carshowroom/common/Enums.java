package com.hsf302.carshowroom.common;

public class Enums {

    public enum Role {
        CUSTOMER,
        STAFF,
        ADMIN;

        @Override public String toString() {
            return switch (this) {
                case CUSTOMER -> "Khách hàng";
                case STAFF -> "Nhân viên";
                case ADMIN -> "Quản trị viên";
            };
        }
    }

    public enum ProductStatus {
        ACTIVE,
        INACTIVE;

        @Override public String toString() { return this == ACTIVE ? "Đang bán" : "Ngừng bán"; }
    }

    public enum ServiceStatus {
        ACTIVE,
        INACTIVE;

        @Override public String toString() { return this == ACTIVE ? "Đang hoạt động" : "Ngừng hoạt động"; }
    }

    public enum FulfillmentType {
        SHIPPING,
        AT_WORKSHOP;

        @Override public String toString() { return this == AT_WORKSHOP ? "Lắp đặt tại xưởng" : "Giao hàng"; }
    }

    public enum OrderType {
        PARENT,
        SHIPPING,
        AT_WORKSHOP;

        @Override public String toString() {
            return switch (this) {
                case PARENT -> "Đơn tổng";
                case SHIPPING -> "Giao hàng";
                case AT_WORKSHOP -> "Tại xưởng";
            };
        }
    }

    public enum OrderStatus {
        CREATED,
        PENDING_PAYMENT,
        PROCESSING,
        SHIPPING,
        COMPLETED,
        CANCELED,
        EXPIRED_PAYMENT;

        @Override public String toString() {
            return switch (this) {
                case CREATED -> "Đã tạo";
                case PENDING_PAYMENT -> "Chờ thanh toán";
                case PROCESSING -> "Đang xử lý";
                case SHIPPING -> "Đang giao hàng";
                case COMPLETED -> "Hoàn tất";
                case CANCELED -> "Đã hủy";
                case EXPIRED_PAYMENT -> "Hết hạn thanh toán";
            };
        }
    }

    public enum BookingStatus {
        CREATED,
        PENDING_PAYMENT,
        CONFIRMED,
        WAITING_FOR_VEHICLE,
        RECEIVING_VEHICLE,
        IN_PROGRESS,
        PENDING_APPROVAL,
        COMPLETED,
        CANCELED,
        EXPIRED_PAYMENT,
        EXPIRED_NO_SHOW;

        @Override public String toString() {
            return switch (this) {
                case CREATED -> "Đã tạo";
                case PENDING_PAYMENT -> "Chờ thanh toán";
                case CONFIRMED -> "Đã xác nhận";
                case WAITING_FOR_VEHICLE -> "Đang đợi xe";
                case RECEIVING_VEHICLE -> "Đang tiếp nhận";
                case IN_PROGRESS -> "Đang thực hiện";
                case PENDING_APPROVAL -> "Chờ duyệt";
                case COMPLETED -> "Hoàn tất";
                case CANCELED -> "Đã hủy";
                case EXPIRED_PAYMENT -> "Hết hạn thanh toán";
                case EXPIRED_NO_SHOW -> "Không đến hẹn";
            };
        }
    }

    public enum BookingType {
        REPAIR_SERVICE,
        PART_INSTALLATION;

        @Override public String toString() {
            return this == PART_INSTALLATION ? "Lắp đặt phụ tùng" : "Sửa chữa / bảo dưỡng";
        }
    }

    public enum PaymentStatus {
        INITIATED,
        PENDING,
        PAID,
        FAILED,
        CANCELED,
        EXPIRED,
        REFUNDED;

        @Override public String toString() {
            return switch (this) {
                case INITIATED -> "Đã khởi tạo";
                case PENDING -> "Đang chờ thanh toán";
                case PAID -> "Đã thanh toán";
                case FAILED -> "Thất bại";
                case CANCELED -> "Đã hủy";
                case EXPIRED -> "Đã hết hạn";
                case REFUNDED -> "Đã hoàn tiền";
            };
        }
    }

    public enum PaymentMethod {
        PAYOS;

        @Override public String toString() {
            return "Thanh toán trực tuyến qua PayOS";
        }
    }

    public enum RefundStatus {
        NONE,
        REQUESTED,
        APPROVED,
        REJECTED,
        PROCESSING,
        COMPLETED,
        FAILED;

        @Override public String toString() {
            return switch (this) {
                case NONE -> "Không có";
                case REQUESTED -> "Chờ hoàn tiền";
                case APPROVED -> "Đã duyệt hoàn tiền";
                case PROCESSING -> "Đang chuyển tiền";
                case COMPLETED -> "Đã hoàn tiền";
                case FAILED -> "Hoàn tiền lỗi";
                case REJECTED -> "Từ chối hoàn tiền";
            };
        }
    }

    public enum RefundPayoutStatus {
        PROCESSING,
        SUCCEEDED,
        FAILED;

        @Override public String toString() {
            return switch (this) {
                case PROCESSING -> "Đang xử lý";
                case SUCCEEDED -> "Đã chuyển tiền";
                case FAILED -> "Hoàn tiền thất bại";
            };
        }
    }

    public enum ReservationStatus {
        HELD,
        CONFIRMED,
        RELEASED,
        CONSUMED,
        EXPIRED;

        @Override public String toString() {
            return switch (this) {
                case HELD -> "Đang giữ chỗ";
                case CONFIRMED -> "Đã xác nhận";
                case RELEASED -> "Đã giải phóng";
                case CONSUMED -> "Đã sử dụng";
                case EXPIRED -> "Đã hết hạn";
            };
        }
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED;

        @Override public String toString() { return switch (this) {
            case PENDING -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Từ chối";
        }; }
    }
}
