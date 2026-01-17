package com.microservices.order.publisher;

import static org.springframework.kafka.support.mapping.AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME;

import com.microservices.order.config.properties.SecurityProperties;
import com.microservices.order.kafka.OrderEventDto;
import com.microservices.order.kafka.OrderEventType;
import com.microservices.order.model.Order;
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
import ru.vvsem.shared.dto.shared_api_dto.UserProfileResponse;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationEventPublisherImpl implements NotificationEventPublisher {

    private final SecurityProperties securityProperties;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.order-events:order.events}")
    private String orderTopic;

    @Override
    @AsyncPublisher(
            operation =
                    @AsyncOperation(
                            payloadType = OrderEventDto.class,
                            channelName = "${app.kafka.topics.order-events:order.events}",
                            description = "Publishes order events",
                            headers =
                                    @AsyncOperation.Headers(
                                            schemaName = "KafkaHeadersDto",
                                            values = {
                                                @AsyncOperation.Headers.Header(
                                                        name = DEFAULT_CLASSID_FIELD_NAME,
                                                        description = "Spring Type Event",
                                                        value = "com.microservices.order.kafka.UserCreatedEvent"),
                                                @AsyncOperation.Headers.Header(
                                                        name = "X-Request-Id",
                                                        description = "Request ID for tracing",
                                                        value = "123e4567-e89b-12d3-a456-426614174000"),
                                                @AsyncOperation.Headers.Header(
                                                        name = "X-Service-Name",
                                                        description = "Service name that produced the message",
                                                        value = "order-service"),
                                                @AsyncOperation.Headers.Header(
                                                        name = "X-Event-Type",
                                                        description = "Type of the event",
                                                        value = "ORDER_CREATED"),
                                                @AsyncOperation.Headers.Header(
                                                        name = "X-Event-Id",
                                                        description = "Unique event ID",
                                                        value = "550e8400-e29b-41d4-a716-446655440000"),
                                                @AsyncOperation.Headers.Header(
                                                        name = "X-Event-Timestamp",
                                                        description = "Event timestamp in ISO format",
                                                        value = "2024-01-13T22:45:48.399Z")
                                            })))
    @KafkaAsyncOperationBinding(clientId = "${spring.kafka.client-id}")
    public void sendNotification(Order order, UserProfileResponse userProfile, String requestId) {
        MDC.clear();
        MDC.put(securityProperties.getRequestIdHeader(), requestId);

        try {
            OrderEventDto eventDto = OrderEventDto.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .email(userProfile.getEmail())
                    .price(order.getPrice())
                    .status(order.getStatus().name())
                    .message(
                            order.getStatus() == Order.Status.PAID ? "Order paid successfully" : "Order payment failed")
                    .build();

            sendEvent(eventDto, requestId);
            log.info("Notification sent for order {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send notification for order {}: {}", order.getId(), e.getMessage(), e);
        }
        MDC.clear();
    }

    private void sendEvent(OrderEventDto eventDto, String requestId) {
        Message<OrderEventDto> message = MessageBuilder.withPayload(eventDto)
                .setHeader(KafkaHeaders.TOPIC, orderTopic.getBytes())
                .setHeader(KafkaHeaders.KEY, eventDto.getUserId().toString())
                .setHeader(
                        "X-OrderEventType",
                        OrderEventType.ORDER_CREATED.toString().getBytes())
                .build();
        kafkaTemplate.send(message).whenComplete((stringObjectSendResult, throwable) -> {
            MDC.clear();
            MDC.put(securityProperties.getRequestIdHeader(), requestId);
            if (throwable != null) {
                log.error(
                        "Failed to send notification for user {}: {}",
                        eventDto.getUserId(),
                        throwable.getMessage(),
                        throwable);
            } else {
                log.info("Sended notification for user {}", eventDto.getUserId());
            }
            MDC.clear();
        });
    }
}
