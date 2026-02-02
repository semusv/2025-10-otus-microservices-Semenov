package com.microservices.billing.service;

import com.microservices.billing.kafka.PaymentEventDto.CompensationResponseEvent;
import com.microservices.billing.kafka.PaymentEventDto.OrderFailedEvent;

public interface PaymentCompensationService {

    public CompensationResponseEvent processCompensationPayment(OrderFailedEvent event);
}
