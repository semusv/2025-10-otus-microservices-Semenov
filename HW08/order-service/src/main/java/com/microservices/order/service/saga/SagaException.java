package com.microservices.order.service.saga;

public class SagaException extends RuntimeException {
    public SagaException(String message, Exception e) {
        super(message, e);
    }
}
