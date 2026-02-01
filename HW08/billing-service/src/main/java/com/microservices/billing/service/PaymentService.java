package com.microservices.billing.service;


import com.microservices.billing.kafka.PaymentEventDto.OrderFailedEvent;
import com.microservices.billing.kafka.PaymentEventDto.PaymentRequestEvent;
import com.microservices.billing.kafka.PaymentEventDto.PaymentResponseEvent;

public interface PaymentService {

    public PaymentResponseEvent processPayment(PaymentRequestEvent event);

    public PaymentResponseEvent processCompensationPayment(OrderFailedEvent event);
}
