package com.microservices.auth.listener;

import com.microservices.auth.config.properties.SecurityProperties;
import com.microservices.auth.event.UserCreatedEvent;
import com.microservices.auth.publisher.AuthEventPublisher;
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
public class AuthEventListenerImpl implements AuthEventListener {

    private final SecurityProperties securityProperties;

    private final AuthEventPublisher publisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Override
    public void handleEvent(UserCreatedEvent event) {
        String requestId = event.mdcContext().get(securityProperties.getRequestIdHeader());
        MDC.clear();
        MDC.put(securityProperties.getRequestIdHeader(), requestId);

        log.debug("Processing event: {}", event);
        publisher.sendUserCreatedEvent(event.user(), requestId);
    }
}
