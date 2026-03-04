package com.microservices.auth.listener;

import com.microservices.auth.event.UserCreatedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public interface AuthEventListener {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleEvent(UserCreatedEvent event);
}
