package com.microservices.delivery.service;

import com.microservices.delivery.model.EventType;

public interface OutboxService {

    void saveEvent(EventType eventType, String aggregateId, String aggregateType, Object payload, String topic);
}
