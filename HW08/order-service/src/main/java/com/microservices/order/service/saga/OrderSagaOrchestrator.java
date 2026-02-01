package com.microservices.order.service.saga;

import com.microservices.order.model.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

public interface OrderSagaOrchestrator {

    void startOrderSaga(Order order);

    @Scheduled(fixedDelay = 300000) // Каждые 5 минут
    @Transactional
    void recoverStuckSagas();
}
