package com.microservices.order.event;

import com.microservices.order.model.Order;
import java.util.Map;

public record OrderCreatedEvent(Order order, String source, Map<String, String> mdcContext) {}
