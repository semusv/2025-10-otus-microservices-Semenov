package com.microservices.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.notification.kafka.OrderEventDto;
import com.microservices.notification.service.NotificationService;
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
public class OrderEventListener {

    private final NotificationService notificationService;

    private final ObjectMapper objectMapper;

    private final Validator validator;

    @KafkaListener(
            topics = "${app.kafka.topics.notification-request:notification.request.v1}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @AsyncListener(
            operation =
                    @AsyncOperation(
                            payloadType = OrderEventDto.class,
                            channelName = "${app.kafka.topics.notification-request:notification.request.v1}",
                            description = "Listener user order created event"))
    @KafkaAsyncOperationBinding(clientId = "${spring.kafka.client-id}")
    public void handleUserCreated(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        try {
            log.info("Received order event. Key: {}, Topic: {}", key, topic);

            // Конвертируем Map в DTO
            OrderEventDto event = convertToDto(payload);

            // Валидируем DTO
            validateEvent(event);

            // Обрабатываем событие
            notificationService.saveFromEvent(event);

            log.info("Successfully processed order event for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error processing order event. Payload: {}", payload, e);
            throw e;
        }
    }

    private OrderEventDto convertToDto(Map<String, Object> payload) {
        try {
            return objectMapper.convertValue(payload, OrderEventDto.class);
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert payload to OrderEventDto: {}", payload, e);
            throw new RuntimeException("Invalid message format", e);
        }
    }

    private void validateEvent(OrderEventDto event) {
        Set<ConstraintViolation<OrderEventDto>> violations = validator.validate(event);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            log.warn("Event validation failed: {}", errorMessage);
            throw new ValidationException("Event validation failed: " + errorMessage);
        }
    }
}
