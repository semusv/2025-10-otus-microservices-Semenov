package com.microservices.auth.service;

import com.microservices.auth.model.User;

public interface EventPublisher {
    void sendUserCreatedEvent(User user);
}
