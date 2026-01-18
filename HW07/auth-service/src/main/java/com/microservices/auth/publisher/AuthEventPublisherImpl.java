package com.microservices.auth.publisher;

import static org.springframework.kafka.support.mapping.AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME;

import com.microservices.auth.config.properties.SecurityProperties;
import com.microservices.auth.kafka.KafkaCustomHeaders;
import com.microservices.auth.kafka.UserEventDto;
import com.microservices.auth.model.User;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class AuthEventPublisherImpl implements AuthEventPublisher {

    private final SecurityProperties securityProperties;

    @Value("${app.kafka.topics.user-created:user.created}")
    private String userCreatedTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @AsyncPublisher(
            operation =
                    @AsyncOperation(
                            payloadType = UserEventDto.class,
                            channelName = "${app.kafka.topics.user-created:user.created}",
                            description = "Publishes user created event",
                            headers =
                                    @AsyncOperation.Headers(
                                            schemaName = "KafkaHeadersDto",
                                            values = {
                                                @AsyncOperation.Headers.Header(
                                                        name = DEFAULT_CLASSID_FIELD_NAME,
                                                        description = "Spring Type Event",
                                                        value = "com.microservices.auth.kafka.UserEventDto"),
                                                @AsyncOperation.Headers.Header(
                                                        name = "X-Source",
                                                        description = "Service name that produced the message",
                                                        value = "auth-service"),
                                                @AsyncOperation.Headers.Header(
                                                        name = "X-Event-Id",
                                                        description = "Unique event ID",
                                                        value = "550e8400-e29b-41d4-a716-446655440000"),
                                                @AsyncOperation.Headers.Header(
                                                        name = "X-Event-Type",
                                                        description = "Type of the event",
                                                        value = "USER_CREATED"),
                                                @AsyncOperation.Headers.Header(
                                                        name = "X-Timestamp",
                                                        description = "Event timestamp in ISO format",
                                                        value = "2024-01-13T22:45:48.399Z"),
                                                @AsyncOperation.Headers.Header(
                                                        name = "X-Request-Id",
                                                        description = "Request ID for tracing",
                                                        value = "123e4567-e89b-12d3-a456-426614174000")
                                            })))
    @KafkaAsyncOperationBinding(clientId = "${spring.kafka.client-id}")
    public void sendUserCreatedEvent(User user, String requestId) {
        Message<UserEventDto> message = prepareMessage(user, requestId);
        kafkaTemplate.send(message).whenComplete((stringObjectSendResult, throwable) -> {
            MDC.clear();
            MDC.put(securityProperties.getRequestIdHeader(), requestId);
            if (throwable != null) {
                log.error(
                        "Failed to send user created event for {}: {}",
                        user.getId(),
                        throwable.getMessage(),
                        throwable);
            } else {
                log.info("Sended user created event for {}", user.getId());
            }
            MDC.clear();
        });
    }

    private Message<UserEventDto> prepareMessage(User user, String requestId) {
        UserEventDto event = UserEventDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
        return MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, userCreatedTopic)
                .setHeader(KafkaHeaders.KEY, user.getId().toString())
                .setHeader(KafkaCustomHeaders.REQUEST_ID, requestId.getBytes())
                .setHeader(
                        KafkaCustomHeaders.EVENT_TYPE,
                        KafkaCustomHeaders.EventType.ORDER_CREATED.toString().getBytes())
                .build();
    }
}
