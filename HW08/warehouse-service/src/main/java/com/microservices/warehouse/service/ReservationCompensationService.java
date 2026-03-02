package com.microservices.warehouse.service;

import com.microservices.warehouse.kafka.SagaEvents.CompensationResponseEvent;
import com.microservices.warehouse.kafka.SagaEvents.OrderFailedEvent;

public interface ReservationCompensationService {
    CompensationResponseEvent processCompensationReservation(OrderFailedEvent event);
}
