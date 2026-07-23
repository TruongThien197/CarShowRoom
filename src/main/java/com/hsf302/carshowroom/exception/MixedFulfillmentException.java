package com.hsf302.carshowroom.exception;

public class MixedFulfillmentException extends RuntimeException {
    public MixedFulfillmentException() {
        super("Mỗi lần thanh toán chỉ hỗ trợ một hình thức nhận hàng. Vui lòng hoàn tất đơn hiện tại hoặc tạo đơn mới với hình thức nhận hàng khác.");
    }
}
