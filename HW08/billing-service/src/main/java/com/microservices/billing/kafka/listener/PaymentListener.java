package com.microservices.billing.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.billing.config.KafkaTopicProperties;
import com.microservices.billing.kafka.PaymentEventDto.PaymentRequestEvent;
import com.microservices.billing.kafka.PaymentEventDto.PaymentResponseEvent;
import com.microservices.billing.model.EventType;
import com.microservices.billing.service.OutboxService;
import com.microservices.billing.service.PaymentService;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentListener {

    private final ObjectMapper objectMapper;

    private final Validator validator;

    private final PaymentService paymentService;

    private final OutboxService outboxService;

    private final KafkaTopicProperties kafkaTopicProperties;

    @KafkaListener(
            topics = "#{@kafkaTopicProperties.getPaymentRequest()}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @AsyncListener(
            operation =
                    @AsyncOperation(
                            payloadType = PaymentRequestEvent.class,
                            channelName = "#{@kafkaTopicProperties.getPaymentRequest()}",
                            description = "Listener payment request"))
    @KafkaAsyncOperationBinding(clientId = "${spring.kafka.client-id}")
    public void handlePaymentRequestEvent(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            log.debug("Received  event. Key: {}, Topic: {}", key, topic);
            PaymentRequestEvent event = convertToDto(payload);
            validateEvent(event);
            PaymentResponseEvent eventResponse = paymentService.processPayment(event);
            outboxService.saveEvent(
                    eventResponse.isSuccess() ? EventType.PAYMENT_COMPLETED : EventType.PAYMENT_FAILED,
                    event.getSagaId().toString(),
                    "Saga",
                    eventResponse,
                    kafkaTopicProperties.getPaymentResponse());

            log.info("Successfully processed payment.request event. UserId: {}", event.getUserId());
        } catch (Exception e) {
            log.error(
                    "Error processing payment.request event. Payload: {}, Key: {}, Topic: {}", payload, key, topic, e);
            throw e;
        }
    }


    private PaymentRequestEvent convertToDto(Map<String, Object> payload) {
        try {
            return objectMapper.convertValue(payload, PaymentRequestEvent.class);
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert payload to PaymentRequestEvent: {}", payload, e);
            throw new RuntimeException("Invalid message format", e);
        }
    }

    private void validateEvent(PaymentRequestEvent event) {
        Set<ConstraintViolation<PaymentRequestEvent>> violations = validator.validate(event);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            log.warn("Event validation failed: {}", errorMessage);
            throw new ValidationException("Event validation failed: " + errorMessage);
        }
    }
}
