package com.microservices.billing.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.billing.kafka.UserEventDto;
import com.microservices.billing.service.AccountService;
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
public class UserCreatedListener {

    private final AccountService accountService;

    private final ObjectMapper objectMapper;

    private final Validator validator;

    @KafkaListener(
            topics = "${app.kafka.topics.user-created:user.created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @AsyncListener(
            operation =
                    @AsyncOperation(
                            payloadType = UserEventDto.class,
                            channelName = "${app.kafka.topics.user-created:user.created}",
                            description = "Listener user created event"))
    @KafkaAsyncOperationBinding(clientId = "${spring.kafka.client-id}")
    public void handleUserCreated(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        try {

            log.debug("Received user.created event. Key: {}, Topic: {}", key, topic);
            // Конвертируем Map в DTO
            UserEventDto event = convertToDto(payload);

            // Валидируем DTO
            validateEvent(event);

            // Обрабатываем событие
            accountService.createAccountIfMissing(event);

            log.info("Successfully processed user.created event for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error processing user.created event. Payload: {}, Key: {}, Topic: {}", payload, key, topic, e);
            throw e;
        }
    }

    private UserEventDto convertToDto(Map<String, Object> payload) {
        try {
            return objectMapper.convertValue(payload, UserEventDto.class);
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert payload to UserEventDto: {}", payload, e);
            throw new RuntimeException("Invalid message format", e);
        }
    }

    private void validateEvent(UserEventDto event) {
        Set<ConstraintViolation<UserEventDto>> violations = validator.validate(event);

        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            log.warn("Event validation failed: {}", errorMessage);
            throw new ValidationException("Event validation failed: " + errorMessage);
        }
    }
}
