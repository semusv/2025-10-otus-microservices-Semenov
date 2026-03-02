package com.microservices.order.service.saga;

import com.microservices.order.model.EventType;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import com.microservices.order.service.saga.steps.SagaStepHandler;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class SagaStateMachine {

    // Изменение: добавили флаг autoTransition
    private record StateConfig(boolean finalState, long timeoutMs, boolean autoTransition) {}

    private final Map<OrderSaga.SagaState, StateConfig> stateConfig;

    private final Map<OrderSaga.SagaState, SagaStepHandler> handlers;

    private final Map<EventType, SagaStepHandler> compesationHandlers;

    private final Map<OrderSaga.SagaState, OrderSaga.SagaState> transitions = Map.of(
            OrderSaga.SagaState.STARTED, OrderSaga.SagaState.PAYMENT_PROCESSING,
            OrderSaga.SagaState.PAYMENT_COMPLETED, OrderSaga.SagaState.WAREHOUSE_RESERVING,
            OrderSaga.SagaState.WAREHOUSE_RESERVED, OrderSaga.SagaState.DELIVERY_RESERVING,
            OrderSaga.SagaState.DELIVERY_RESERVED, OrderSaga.SagaState.COMPLETED);

    public SagaStateMachine(List<SagaStepHandler> handlers) {
        this.handlers =
                handlers.stream().collect(Collectors.toMap(SagaStepHandler::getHandledState, Function.identity()));

        this.compesationHandlers = handlers.stream()
                .collect(Collectors.toMap(SagaStepHandler::getHandledCompensateEventType, Function.identity()));

        // Изменение: добавили autoTransition флаг в конфигурацию
        this.stateConfig = Map.of(
                OrderSaga.SagaState.STARTED, new StateConfig(false, 0L, true),
                OrderSaga.SagaState.PAYMENT_PROCESSING, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.WAREHOUSE_RESERVING, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.DELIVERY_RESERVING, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.COMPLETED, new StateConfig(true, 0L, false),
                OrderSaga.SagaState.COMPENSATED, new StateConfig(true, 0L, false),
                OrderSaga.SagaState.COMPENSATING, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.PAYMENT_FAILED, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.WAREHOUSE_FAILED, new StateConfig(false, 10_000L, false),
                OrderSaga.SagaState.DELIVERY_FAILED, new StateConfig(false, 10_000L, false));

        log.debug("SagaStateMachine initialized with {} handlers", handlers.size());
    }

    @Transactional
    public void process(OrderSaga saga, Order order) {
        saga.setRetryCount(0);
        OrderSaga.SagaState nextState = transitions.get(saga.getState());
        executeStep(saga, order, nextState);
    }

    @Transactional
    public void compensate(OrderSaga saga, Order order, String reason, EventType eventType) {
        SagaStepHandler handler = compesationHandlers.get(eventType);

        if (handler == null) {
            throw new IllegalStateException("No handler for compensate eventType: " + eventType.toString());
        }

        log.debug("Executing compensate {} for saga {}", eventType.toString(), saga.getSagaId());
        handler.compensate(saga, order, reason);
    }

    @Transactional
    public void retryStep(OrderSaga saga, Order order) {
        executeStep(saga, order, saga.getState());
    }

    private void executeStep(OrderSaga saga, Order order, OrderSaga.SagaState nexState) {
        SagaStepHandler handler = handlers.get(nexState);

        if (handler == null) {
            throw new IllegalStateException("No handler for nexState: " + nexState);
        }

        if (getConfig(nexState).finalState()) {
            log.info("Sagа {} has reached FINAL state: {}", saga.getSagaId(), nexState);
        }

        log.debug("Executing {} for saga {}", nexState, saga.getSagaId());
        handler.execute(saga, order);
    }

    public long getTimeoutForState(OrderSaga.SagaState state) {
        return getConfig(state).timeoutMs();
    }

    private StateConfig getConfig(OrderSaga.SagaState state) {
        return stateConfig.getOrDefault(state, new StateConfig(true, 60_000L, false));
    }
}
