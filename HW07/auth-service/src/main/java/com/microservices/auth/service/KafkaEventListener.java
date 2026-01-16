package com.microservices.auth.service;

import com.microservices.auth.config.properties.SecurityProperties;
import com.microservices.auth.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventListener {

    private final SecurityProperties securityProperties;

    private final KafkaEventPublisher publisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(UserCreatedEvent event) {
        MDC.clear();
        MDC.put(securityProperties.getRequestIdHeader(), event.requestId());

        log.debug("Processing event: {}", event);
        publisher.sendUserCreatedEvent(event.user(), event.requestId());
    }
}
