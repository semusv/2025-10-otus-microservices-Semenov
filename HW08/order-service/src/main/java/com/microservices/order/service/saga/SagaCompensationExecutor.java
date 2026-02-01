package com.microservices.order.service.saga;

import com.microservices.order.config.KafkaTopicProperties;
import com.microservices.order.kafka.SagaEvents;
import com.microservices.order.model.EventType;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCompensationExecutor {

    private final OutboxService outboxService;

    private final OrderRepository orderRepository;

    private final KafkaTopicProperties kafkaTopicProperties;

    /**
     * Выполняет компенсацию шагов саги в обратном порядке
     */
    public void executeCompensation(OrderSaga saga, Order order, String reason) {
        log.info("Executing compensation for saga {}: {}", saga.getSagaId(), reason);

        if (saga.isFullyCompensated()) {
            saga.setState(OrderSaga.SagaState.COMPENSATED);
            return;
        }

        if (saga.isPaymentExecuted()) {
            sendPaymentCompensate(order, reason);
        }
        if (saga.isWarehouseExecuted()) {
            sendWarehouseCompensate(order, reason);
        }
        if (saga.isDeliveryExecuted()) {
            sendDeliveryCompensate(order, reason);
        }
    }

    private void sendPaymentCompensate(Order order, String reason) {
        SagaEvents.OrderFailedEvent failedEvent = SagaEvents.OrderFailedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .reason(reason)
                .build();

        outboxService.saveEvent(
                EventType.PAYMENT_REFUNDED,
                order.getId().toString(),
                "saga",
                failedEvent,
                kafkaTopicProperties.getPaymentCompensateRequest());
    }

    private void sendWarehouseCompensate(Order order, String reason) {
        SagaEvents.OrderFailedEvent failedEvent = SagaEvents.OrderFailedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .reason(reason)
                .build();

        outboxService.saveEvent(
                EventType.PAYMENT_REFUNDED,
                order.getId().toString(),
                "saga",
                failedEvent,
                kafkaTopicProperties.getWarehouseCompensateRequest());
    }

    private void sendDeliveryCompensate(Order order, String reason) {
        SagaEvents.OrderFailedEvent failedEvent = SagaEvents.OrderFailedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .reason(reason)
                .build();

        outboxService.saveEvent(
                EventType.PAYMENT_REFUNDED,
                order.getId().toString(),
                "saga",
                failedEvent,
                kafkaTopicProperties.getDeliveryCompensateRequest());
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
