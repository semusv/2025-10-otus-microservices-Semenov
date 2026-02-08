package com.microservices.delivery.service;

import com.microservices.delivery.kafka.SagaEvents.DeliveryReservationRequestEvent;
import com.microservices.delivery.kafka.SagaEvents.DeliveryReservationResponseEvent;

public interface ReservationService {
    DeliveryReservationResponseEvent processReservation(DeliveryReservationRequestEvent event);
}
