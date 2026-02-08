package com.microservices.order.service.saga;

import com.microservices.order.model.EventType;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCompensationExecutor {

    private final SagaStateMachine sagaStateMachine;

    /**
     * Выполняет компенсацию шагов саги в обратном порядке
     */
    @Transactional
    public void executeCompensation(OrderSaga saga, Order order, String reason) {
        log.info("Executing compensation for saga {}: {}", saga.getSagaId(), reason);

        if (saga.isFullyCompensated()) {
            // Финальная информация о компенсации
            fullCompensate(saga, order);
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
    public void compensatePayment(OrderSaga saga, Order order) {
        saga.markPaymentExecuted(false);
        if (saga.isFullyCompensated()) {
            fullCompensate(saga, order);
        }
    }

    @Transactional
    public void compensateWarehouse(OrderSaga saga, Order order) {
        saga.markWarehouseExecuted(false);
        if (saga.isFullyCompensated()) {
            fullCompensate(saga, order);
        }
    }

    @Transactional
    public void compensateDelivery(OrderSaga saga, Order order) {
        saga.markDeliveryExecuted(false);
        if (saga.isFullyCompensated()) {
            fullCompensate(saga, order);
        }
    }

    private void fullCompensate(OrderSaga saga, Order order) {
        log.info("Sending full compensation for saga {}", saga.getSagaId());
        order.setStatus(Order.Status.CANCELLED);
        try {
            String compensationReason = "Fully compensated";
            sagaStateMachine.compensate(saga, order, compensationReason, EventType.COMPENSATING_REQUESTED);
            saga.setState(OrderSaga.SagaState.COMPENSATED);
        } catch (Exception e) {
            log.error("FULLY Compensation failed for saga {}: {}", saga.getSagaId(), e.getMessage());
        }
    }
}
