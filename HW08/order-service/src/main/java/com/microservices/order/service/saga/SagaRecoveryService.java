package com.microservices.order.service.saga;

import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.repository.SagaRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaRecoveryService {

    private final SagaRepository sagaRepository;

    private final OrderRepository orderRepository;

    private final SagaCompensationExecutor compensationExecutor;

    public void recoverStuckSagas(SagaStateMachine stateMachine) {
        List<OrderSaga> stuckSagas = findStuckSagas(stateMachine);
        if (!stuckSagas.isEmpty()) {
            log.warn("Found {} stuck sagas for recovery", stuckSagas.size());
            for (OrderSaga saga : stuckSagas) {
                // Если превышены попытки - запускаем компенсацию
                if (saga.getRetryCount() < 3) { // 0-based
                    recoverSaga(saga, stateMachine);
                } else {
                    Order order = getOrder(saga.getOrderId());
                    compensationExecutor.executeCompensation(
                            saga, order, "Failed to recover saga after maximum retries");
                }
                sagaRepository.save(saga);
            }
        }
    }

    public void compensateFailedSagas(SagaStateMachine stateMachine) {
        List<OrderSaga> stuckSagas = findFailedSagas(stateMachine);

        if (!stuckSagas.isEmpty()) {
            log.warn("Found {} failed sagas for compensation", stuckSagas.size());
            for (OrderSaga saga : stuckSagas) {
                Order order = getOrder(saga.getOrderId());
                String errorMessage = getErrorCompensatedMessage(saga);
                compensationExecutor.executeCompensation(saga, order, errorMessage);
                sagaRepository.save(saga);
            }
        }
    }

    private static String getErrorCompensatedMessage(OrderSaga saga) {
        String errorMessage = "Unknown error";
        if (saga.getState() == OrderSaga.SagaState.COMPENSATING) {
            errorMessage = "Compensation repeat";
        } else if (saga.getState() == OrderSaga.SagaState.PAYMENT_FAILED) {
            errorMessage = "Payment failed";
        } else if (saga.getState() == OrderSaga.SagaState.WAREHOUSE_FAILED) {
            errorMessage = "Warehouse failed";
        } else if (saga.getState() == OrderSaga.SagaState.DELIVERY_FAILED) {
            errorMessage = "Delivery failed";
        }
        return errorMessage;
    }

    private List<OrderSaga> findFailedSagas(SagaStateMachine stateMachine) {
        // Находим саги, которые ожидают ответов
        List<OrderSaga> sagasAwaitingResponse = sagaRepository.findByStateIn(List.of(
                OrderSaga.SagaState.PAYMENT_FAILED,
                OrderSaga.SagaState.WAREHOUSE_FAILED,
                OrderSaga.SagaState.DELIVERY_FAILED,
                OrderSaga.SagaState.COMPENSATING));
        // Фильтруем те, у которых истек таймаут
        return sagasAwaitingResponse.stream()
                .filter(saga -> {
                    long timeout = stateMachine.getTimeoutForState(saga.getState());
                    return saga.getUpdatedAt().isBefore(LocalDateTime.now().minus(timeout, ChronoUnit.MILLIS));
                })
                .toList();
    }

    private List<OrderSaga> findStuckSagas(SagaStateMachine stateMachine) {
        // Находим саги, которые ожидают ответов
        List<OrderSaga> sagasAwaitingResponse = sagaRepository.findByStateIn(List.of(
                OrderSaga.SagaState.PAYMENT_PROCESSING,
                OrderSaga.SagaState.WAREHOUSE_RESERVING,
                OrderSaga.SagaState.DELIVERY_SCHEDULING));

        // Фильтруем те, у которых истек таймаут
        return sagasAwaitingResponse.stream()
                .filter(saga -> {
                    long timeout = stateMachine.getTimeoutForState(saga.getState());
                    return saga.getUpdatedAt().isBefore(LocalDateTime.now().minus(timeout, ChronoUnit.MILLIS));
                })
                .toList();
    }

    private void recoverSaga(OrderSaga saga, SagaStateMachine stateMachine) {
        Order order = getOrder(saga.getOrderId());
        try {
            saga.setRetryCount(saga.getRetryCount() + 1);
            log.warn(
                    "Recovering saga {} in state {} (retry {}/3)",
                    saga.getSagaId(),
                    saga.getState(),
                    saga.getRetryCount());
            // Просто повторно выполняем текущий шаг
            stateMachine.retryStep(saga, order);

        } catch (Exception e) {
            log.error("Failed to recover saga {}", saga.getSagaId(), e);
        }
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
}
