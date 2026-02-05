package com.microservices.warehouse.service;

import com.microservices.warehouse.kafka.SagaEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ReservationCompensationServiceImpl implements ReservationCompensationService {
    @Override
    public SagaEvents.CompensationResponseEvent processCompensationReservation(SagaEvents.OrderFailedEvent event) {

        // TODO: Implement compensation logic
        return null;
    }
}
