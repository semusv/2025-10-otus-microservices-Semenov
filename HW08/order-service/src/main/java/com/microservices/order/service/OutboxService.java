package com.microservices.order.service;

import com.microservices.order.model.EventType;

public interface OutboxService {

    void saveEvent(EventType eventType, String aggregateId, String aggregateType, Object payload, String topic);

    void processOutbox();
}
