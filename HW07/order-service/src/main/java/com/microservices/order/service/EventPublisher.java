package com.microservices.order.service;

import com.microservices.order.model.Order;

public interface EventPublisher {
    void sendNotification(Order order);
}
