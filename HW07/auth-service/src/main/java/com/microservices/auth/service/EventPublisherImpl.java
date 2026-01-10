package com.microservices.auth.service;

import com.microservices.auth.kafka.UserCreatedEvent;
import com.microservices.auth.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class EventPublisherImpl implements EventPublisher {

    @Value("${app.kafka.topics.user-created:user.created}")
    private String userCreatedTopic;

    private final KafkaTemplate<String, UserCreatedEvent> kafkaTemplate;

    @Override
    @Async
    public void sendUserCreatedEvent(User user) {
        try {
            UserCreatedEvent event = UserCreatedEvent.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .build();
            kafkaTemplate.send(userCreatedTopic, user.getId().toString(), event);
            log.info("Sended user created event for {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send user created event for {}: {}", user.getId(), e.getMessage(), e);
        }
    }
}
