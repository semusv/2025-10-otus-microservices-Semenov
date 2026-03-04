package com.microservices.warehouse.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.warehouse.config.KafkaTopicProperties;
import com.microservices.warehouse.kafka.SagaEvents.WarehouseReservationRequestEvent;
import com.microservices.warehouse.kafka.SagaEvents.WarehouseReservationResponseEvent;
import com.microservices.warehouse.model.EventType;
import com.microservices.warehouse.service.OutboxService;
import com.microservices.warehouse.service.ReservationService;
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
public class ReservationListener {

    private final ObjectMapper objectMapper;

    private final Validator validator;

    private final ReservationService reservationService;

    private final OutboxService outboxService;

    private final KafkaTopicProperties kafkaTopicProperties;

    @KafkaListener(
            topics = "#{@kafkaTopicProperties.getWarehouseReservationRequest()}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @AsyncListener(
            operation =
                    @AsyncOperation(
                            payloadType = WarehouseReservationRequestEvent.class,
                            channelName = "#{@kafkaTopicProperties.getWarehouseReservationRequest()}",
                            description = "Listener Reservation request"))
    @KafkaAsyncOperationBinding(clientId = "${spring.kafka.client-id}")
    public void handleReservationRequestEvent(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            log.debug("Received  event. Key: {}, Topic: {}", key, topic);
            WarehouseReservationRequestEvent event = convertToDto(payload);
            validateEvent(event);
            WarehouseReservationResponseEvent eventResponse = reservationService.processReservation(event);
            outboxService.saveEvent(
                    eventResponse.isSuccess()
                            ? EventType.WAREHOUSE_RESERVATION_COMPLETED
                            : EventType.WAREHOUSE_RESERVATION_FAILED,
                    event.getSagaId().toString(),
                    "Saga",
                    eventResponse,
                    kafkaTopicProperties.getWarehouseReservationResponse());
            log.info("Successfully processed Reservation.request event. UserId: {}", event.getUserId());
        } catch (Exception e) {
            log.error(
                    "Error processing Reservation.request event. Payload: {}, Key: {}, Topic: {}",
                    payload,
                    key,
                    topic,
                    e);
            throw e;
        }
    }

    private WarehouseReservationRequestEvent convertToDto(Map<String, Object> payload) {
        try {
            return objectMapper.convertValue(payload, WarehouseReservationRequestEvent.class);
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert payload to ReservationRequestEvent: {}", payload, e);
            throw new RuntimeException("Invalid message format", e);
        }
    }

    private void validateEvent(WarehouseReservationRequestEvent event) {
        Set<ConstraintViolation<WarehouseReservationRequestEvent>> violations = validator.validate(event);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            log.warn("Event validation failed: {}", errorMessage);
            throw new ValidationException("Event validation failed: " + errorMessage);
        }
    }
}
