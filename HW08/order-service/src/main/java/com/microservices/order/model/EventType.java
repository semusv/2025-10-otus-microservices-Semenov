package com.microservices.order.model;

public enum EventType {
    // Payment
    PAYMENT_REQUESTED,
    PAYMENT_REFUNDED,

    // Warehouse
    WAREHOUSE_RESERVATION_REQUESTED,
    WAREHOUSE_RESERVATION_CANCELED,

    // Delivery
    DELIVERY_RESERVATION_REQUESTED,
    DELIVERY_RESERVATION_CANCELED,

}
