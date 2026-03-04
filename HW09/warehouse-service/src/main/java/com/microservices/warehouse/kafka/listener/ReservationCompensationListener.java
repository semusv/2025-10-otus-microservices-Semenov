package com.microservices.warehouse.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.warehouse.config.KafkaTopicProperties;
import com.microservices.warehouse.kafka.SagaEvents.CompensationResponseEvent;
import com.microservices.warehouse.kafka.SagaEvents.OrderFailedEvent;
import com.microservices.warehouse.model.EventType;
import com.microservices.warehouse.service.OutboxService;
import com.microservices.warehouse.service.ReservationCompensationService;
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
public class ReservationCompensationListener {

    private final ObjectMapper objectMapper;

    private final Validator validator;

    private final ReservationCompensationService reservationCompensationService;

    private final OutboxService outboxService;

    private final KafkaTopicProperties kafkaTopicProperties;

    @KafkaListener(
            topics = "#{@kafkaTopicProperties.getWarehouseReservationCompensationRequest()}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @AsyncListener(
            operation =
                    @AsyncOperation(
                            payloadType = OrderFailedEvent.class,
                            channelName = "#{@kafkaTopicProperties.getWarehouseReservationCompensationRequest()}",
                            description = "Listener for Reservation compensation request"))
    @KafkaAsyncOperationBinding(clientId = "${spring.kafka.client-id}")
    public void handleReservationCompensationRequestEvent(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            log.debug("Received Reservation compensation event. Key: {}, Topic: {}", key, topic);
            OrderFailedEvent event = convertToDto(payload);
            validateEvent(event);

            // Обработка компенсации
            CompensationResponseEvent responseEvent =
                    reservationCompensationService.processCompensationReservation(event);

            // Сохраняем событие в outbox
            outboxService.saveEvent(
                    responseEvent.isSuccess()
                            ? EventType.WAREHOUSE_RESERVATION_COMPENSATION_COMPLETED
                            : EventType.WAREHOUSE_RESERVATION_COMPENSATION_FAILED,
                    event.getSagaId().toString(),
                    "Saga",
                    responseEvent,
                    kafkaTopicProperties.getWarehouseReservationCompensationResponse());
            log.info("Successfully processed Reservation compensation. SagaId: {}", event.getSagaId());
        } catch (Exception e) {
            log.error("Error processing Reservation compensation event. Key: {}, Topic: {}", key, topic, e);
            throw e; // Переброс исключения для повтора
        }
    }

    private OrderFailedEvent convertToDto(Map<String, Object> payload) {
        try {
            return objectMapper.convertValue(payload, OrderFailedEvent.class);
        } catch (Exception e) {
            log.error("Failed to convert payload to OrderFailedEvent: {}", payload, e);
            throw new RuntimeException("Invalid message format", e);
        }
    }

    private void validateEvent(OrderFailedEvent event) {
        Set<ConstraintViolation<OrderFailedEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            log.warn("Event validation failed: {}", errorMessage);
            throw new ValidationException("Event validation failed: " + errorMessage);
        }
    }
}
