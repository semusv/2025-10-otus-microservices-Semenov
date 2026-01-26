package com.microservices.order.service;

import com.microservices.order.model.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

public interface OrderSagaOrchestrator {

    void startOrderSaga(Order order);

    void handlePaymentResponse(String message);

    void handleWarehouseResponse(String message);

    void handleDeliveryResponse(String message);

    @Scheduled(fixedDelay = 300000) // Каждые 5 минут
    @Transactional
    void recoverStuckSagas();
}
