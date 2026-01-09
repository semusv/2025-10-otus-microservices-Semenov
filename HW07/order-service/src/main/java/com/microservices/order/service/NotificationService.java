package com.microservices.order.service;

import com.microservices.order.model.Order;

public interface NotificationService {
    void sendNotification(Order order);
}
