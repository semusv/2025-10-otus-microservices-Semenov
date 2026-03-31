package com.microservices.order.service.saga.steps;

import com.microservices.order.client.UserServiceClient;
import com.microservices.order.config.KafkaTopicProperties;
import com.microservices.order.kafka.OrderEventDto;
import com.microservices.order.model.EventType;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import com.microservices.order.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinalStepHandler implements SagaStepHandler {

    private static final OrderSaga.SagaState SAGA_STATE = OrderSaga.SagaState.COMPLETED;

    private static final EventType EVENT_TYPE = EventType.NOTIFICATION_REQUESTED;

    private static final EventType COMPENSATE_EVENT_TYPE = EventType.COMPENSATING_REQUESTED;

    private final OutboxService outboxService;

    private final KafkaTopicProperties kafkaTopicProperties;

    private final UserServiceClient userClient;

    @Override
    public void execute(OrderSaga saga, Order order) {
        saga.setState(SAGA_STATE);
        sendSuccessNotification(saga, order);
    }

    private void sendSuccessNotification(OrderSaga saga, Order order) {
        OrderEventDto eventDto = prepareNotificationEvent(order, true);
        outboxService.saveEvent(
                EVENT_TYPE,
                saga.getSagaId().toString(),
                "Saga",
                eventDto,
                kafkaTopicProperties.getNotificationRequest());
        log.info("Notification request sent for order {} via saga {}", order.getId(), saga.getSagaId());
    }

    private OrderEventDto prepareNotificationEvent(Order order, boolean isSuccess) {
        var userProfile = userClient.fetchMyProfile(order.getUserId());

        return OrderEventDto.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .email(userProfile.getEmail())
                .price(order.getPrice())
                .status(OrderEventDto.Status.valueOf(order.getStatus().name()))
                .message(isSuccess ? "Order created successfully" : "Order creation failed")
                .build();
    }

    @Override
    public void compensate(OrderSaga saga, Order order, String reason) {
        OrderEventDto eventDto = prepareNotificationEvent(order, false);
        outboxService.saveEvent(
                EVENT_TYPE, order.getId().toString(), "saga", eventDto, kafkaTopicProperties.getNotificationRequest());
    }

    @Override
    public boolean canHandle(OrderSaga.SagaState state) {
        return state == SAGA_STATE;
    }

    @Override
    public OrderSaga.SagaState getHandledState() {
        return SAGA_STATE;
    }

    @Override
    public EventType getHandledCompensateEventType() {
        return COMPENSATE_EVENT_TYPE;
    }
}
