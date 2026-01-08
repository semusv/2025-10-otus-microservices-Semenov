package com.microservices.billing.kafka;

import com.microservices.billing.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedListener {

    private final AccountService accountService;

    @KafkaListener(topics = "${app.kafka.topics.user-created:user.created}", groupId = "billing-service")
    public void onUserCreated(@Payload UserCreatedEvent event) {
        log.info("Received user created event: {}", event.getUserId());
        accountService.createAccountIfMissing(event);
    }
}
