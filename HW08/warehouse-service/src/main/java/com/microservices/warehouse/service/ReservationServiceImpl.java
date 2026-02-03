package com.microservices.warehouse.service;

import com.microservices.warehouse.kafka.SagaEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ReservationServiceImpl implements ReservationService {
    @Override
    public SagaEvents.WarehouseReservationResponseEvent processReservation(
            SagaEvents.WarehouseReservationRequestEvent event) {
        return null;
    }
}
