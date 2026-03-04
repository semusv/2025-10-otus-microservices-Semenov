package com.microservices.billing.service;

import com.microservices.billing.model.EventType;

public interface OutboxService {

    void saveEvent(EventType eventType, String aggregateId, String aggregateType, Object payload, String topic);

    void processOutbox();
}
