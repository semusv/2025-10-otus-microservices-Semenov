package com.microservices.delivery.service;

import com.microservices.delivery.kafka.SagaEvents.CompensationResponseEvent;
import com.microservices.delivery.kafka.SagaEvents.OrderFailedEvent;

public interface ReservationCompensationService {
    CompensationResponseEvent processCompensationReservation(OrderFailedEvent event);
}
