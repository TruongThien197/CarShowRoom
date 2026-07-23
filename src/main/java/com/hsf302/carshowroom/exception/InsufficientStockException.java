package com.hsf302.carshowroom.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(int availableStock) {
        super("Không đủ tồn kho. Vui lòng giảm số lượng xuống tối đa " + availableStock + ".");
    }
}
