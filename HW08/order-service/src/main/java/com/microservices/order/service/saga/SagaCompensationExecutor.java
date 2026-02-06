package com.microservices.order.service.saga;

import com.microservices.order.config.KafkaTopicProperties;
import com.microservices.order.model.EventType;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import com.microservices.order.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
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
        sagaStateMachine.compensate(saga, order, reason, EventType.DELIVERY_RESERVATION_CANCELED);
    }

    @Transactional
    public void compensatePayment(OrderSaga saga) {
        saga.markPaymentExecuted();
        if (saga.isFullyCompensated()) {
            saga.setState(OrderSaga.SagaState.COMPENSATED);
        }
    }

    @Transactional
    public void compensateWarehouse(OrderSaga saga) {
        saga.markWarehouseExecuted();
        if (saga.isFullyCompensated()) {
            saga.setState(OrderSaga.SagaState.COMPENSATED);
        }
    }

    @Transactional
    public void compensateDelivery(OrderSaga saga) {
        saga.markDeliveryExecuted();
        if (saga.isFullyCompensated()) {
            saga.setState(OrderSaga.SagaState.COMPENSATED);
        }
    }
}
