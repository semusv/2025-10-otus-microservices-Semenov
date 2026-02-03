package com.microservices.warehouse.service;

import com.microservices.warehouse.kafka.SagaEvents.WarehouseReservationRequestEvent;
import com.microservices.warehouse.kafka.SagaEvents.WarehouseReservationResponseEvent;

public interface ReservationService {
    WarehouseReservationResponseEvent processReservation(WarehouseReservationRequestEvent event);
}
