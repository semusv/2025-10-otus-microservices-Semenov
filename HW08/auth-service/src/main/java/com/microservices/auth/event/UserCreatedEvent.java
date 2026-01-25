package com.microservices.auth.event;

import com.microservices.auth.model.User;
import java.util.Map;

public record UserCreatedEvent(User user, String source, Map<String, String> mdcContext) {}
