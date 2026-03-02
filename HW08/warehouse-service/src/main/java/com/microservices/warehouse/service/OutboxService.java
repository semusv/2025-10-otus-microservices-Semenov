package com.microservices.warehouse.service;

import com.microservices.warehouse.model.EventType;

public interface OutboxService {

    void saveEvent(EventType eventType, String aggregateId, String aggregateType, Object payload, String topic);

    void processOutbox();
}
