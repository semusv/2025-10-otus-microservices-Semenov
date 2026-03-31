package com.microservices.order.service.saga.steps;

import com.microservices.order.config.KafkaTopicProperties;
import com.microservices.order.kafka.SagaEvents;
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
public class PaymentStepHandler implements SagaStepHandler {

    private static final OrderSaga.SagaState SAGA_STATE = OrderSaga.SagaState.PAYMENT_PROCESSING;

    private static final EventType EVENT_TYPE = EventType.PAYMENT_REQUESTED;

    private static final EventType COMPENSATE_EVENT_TYPE = EventType.PAYMENT_REFUNDED;

    private final OutboxService outboxService;

    private final KafkaTopicProperties kafkaTopicProperties;

    @Override
    public void execute(OrderSaga saga, Order order) {
        saga.setState(SAGA_STATE);

        SagaEvents.PaymentRequestEvent paymentEvent = SagaEvents.PaymentRequestEvent.builder()
                .sagaId(saga.getSagaId())
                .orderId(order.getId())
                .userId(order.getUserId())
                .amount(order.getPrice())
                .description("Payment for order #" + order.getId())
                .build();

        outboxService.saveEvent(
                EVENT_TYPE,
                saga.getSagaId().toString(),
                "Saga",
                paymentEvent,
                kafkaTopicProperties.getRequestTopic(saga.getState()));

        log.info("Payment requested for order {} via saga {}", order.getId(), saga.getSagaId());
    }

    @Override
    public void compensate(OrderSaga saga, Order order, String reason) {
        SagaEvents.OrderFailedEvent failedEvent = SagaEvents.OrderFailedEvent.builder()
                .sagaId(saga.getSagaId())
                .orderId(order.getId())
                .userId(order.getUserId())
                .reason(reason)
                .build();

        outboxService.saveEvent(
                COMPENSATE_EVENT_TYPE,
                order.getId().toString(),
                "saga",
                failedEvent,
                kafkaTopicProperties.getPaymentCompensationRequest());
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
