package com.portfolio.inventory.exception;

public class StaleTransferOrderException extends RuntimeException {
    public StaleTransferOrderException(String message) {
        super(message);
    }
}
