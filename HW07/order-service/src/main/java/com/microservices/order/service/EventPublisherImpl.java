package com.microservices.order.service;

import static org.springframework.kafka.support.mapping.AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME;

import com.microservices.order.client.UserServiceClient;
import com.microservices.order.kafka.EventType;
import com.microservices.order.kafka.OrderEvent;
import com.microservices.order.model.Order;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class EventPublisherImpl implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final UserServiceClient userClient;

    @Value("${app.kafka.topics.order-events:order.events}")
    private String orderTopic;

    @Override
    @Async
    @AsyncPublisher(
            operation =
                    @AsyncOperation(
                            payloadType = OrderEvent.class,
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
    public void sendNotification(Order order) {
        try {
            String email = userClient.fetchMyProfile(order.getUserId()).getEmail();
            sendEvent(order, email);
            log.info("Notification sent for order {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to send notification for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    private void sendEvent(Order order, String email) {
        String statusText = order.getStatus() == Order.Status.PAID ? "Order paid successfully" : "Order payment failed";

        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .email(email)
                .price(order.getPrice())
                .status(order.getStatus().name())
                .message(statusText)
                .build();

        Message<OrderEvent> message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, orderTopic.getBytes())
                .setHeader(KafkaHeaders.KEY, order.getUserId().toString())
                .setHeader("X-EventType", EventType.ORDER_CREATED.toString().getBytes())
                .build();

        kafkaTemplate.send(message);
    }
}
