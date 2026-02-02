package com.microservices.billing.model;

public enum EventType {
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,

    PAYMENT_COMPENSATION_COMPLETED,
    PAYMENT_COMPENSATION_FAILED;
}
