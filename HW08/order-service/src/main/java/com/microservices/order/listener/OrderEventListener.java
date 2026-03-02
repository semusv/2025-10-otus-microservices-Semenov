package com.microservices.order.listener;

import com.microservices.order.event.OrderCreatedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public interface OrderEventListener {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleEvent(OrderCreatedEvent event);
}
