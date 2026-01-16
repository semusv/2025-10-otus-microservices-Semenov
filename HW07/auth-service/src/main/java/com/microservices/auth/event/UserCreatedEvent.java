package com.microservices.auth.event;

import com.microservices.auth.model.User;

public record UserCreatedEvent(User user, String source, String requestId) {}
