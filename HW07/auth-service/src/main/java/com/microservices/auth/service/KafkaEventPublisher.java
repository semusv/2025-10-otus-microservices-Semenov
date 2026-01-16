package com.microservices.auth.service;

import com.microservices.auth.model.User;

public interface KafkaEventPublisher {
    void sendUserCreatedEvent(User user, String requestId);
}
