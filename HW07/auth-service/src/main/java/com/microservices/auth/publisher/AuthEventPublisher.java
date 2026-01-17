package com.microservices.auth.publisher;

import com.microservices.auth.model.User;

public interface AuthEventPublisher {
    void sendUserCreatedEvent(User user, String requestId);
}
