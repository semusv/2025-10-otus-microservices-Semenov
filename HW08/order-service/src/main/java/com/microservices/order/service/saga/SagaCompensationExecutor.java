package com.microservices.order.service.saga;

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
public class SagaCompensationExecutor {

    private final OutboxService outboxService;

    private final KafkaTopicProperties kafkaTopicProperties;

    private final SagaStateMachine sagaStateMachine;

    /**
     * Выполняет компенсацию шагов саги в обратном порядке
     */
    public void executeCompensation(OrderSaga saga, Order order, String reason) {
        log.info("Executing compensation for saga {}: {}", saga.getSagaId(), reason);

        if (saga.isFullyCompensated()) {
            saga.setState(OrderSaga.SagaState.COMPENSATED);
            return;
        }

        saga.setState(OrderSaga.SagaState.COMPENSATING);

        if (saga.isPaymentExecuted()) {
            sendPaymentCompensate(saga, order, reason);
        }
        if (saga.isWarehouseExecuted()) {
            sendWarehouseCompensate(saga, order, reason);
        }
        if (saga.isDeliveryExecuted()) {
            sendDeliveryCompensate(saga, order, reason);
        }
    }

    private void sendPaymentCompensate(OrderSaga saga, Order order, String reason) {
        sagaStateMachine.compensate(saga, order, reason, EventType.PAYMENT_REFUNDED);
    }

    private void sendWarehouseCompensate(OrderSaga saga, Order order, String reason) {
        sagaStateMachine.compensate(saga, order, reason, EventType.WAREHOUSE_RESERVATION_CANCELED);
    }

    private void sendDeliveryCompensate(OrderSaga saga, Order order, String reason) {
        SagaEvents.OrderFailedEvent failedEvent = SagaEvents.OrderFailedEvent.builder()
                .sagaId(saga.getSagaId())
                .orderId(order.getId())
                .userId(order.getUserId())
                .reason(reason)
                .build();

        outboxService.saveEvent(
                EventType.DELIVERY_CANCELLED,
                order.getId().toString(),
                "saga",
                failedEvent,
                kafkaTopicProperties.getDeliveryCompensationRequest());
    }

    public void compensatePayment(OrderSaga saga) {
        saga.markPaymentExecuted();
        if (saga.isFullyCompensated()) {
            saga.setState(OrderSaga.SagaState.COMPENSATED);
        }
    }

    public void compensateWarehouse(OrderSaga saga) {
        saga.markWarehouseExecuted();
        if (saga.isFullyCompensated()) {
            saga.setState(OrderSaga.SagaState.COMPENSATED);
        }
    }

    public void compensateDelivery(OrderSaga saga) {
        saga.markDeliveryExecuted();
        if (saga.isFullyCompensated()) {
            saga.setState(OrderSaga.SagaState.COMPENSATED);
        }
    }
}
