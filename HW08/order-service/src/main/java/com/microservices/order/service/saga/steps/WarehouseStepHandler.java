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
public class WarehouseStepHandler implements SagaStepHandler {

    private static final OrderSaga.SagaState SAGA_STATE = OrderSaga.SagaState.WAREHOUSE_RESERVING;

    private static final EventType EVENT_TYPE = EventType.WAREHOUSE_REQUESTED;

    private final OutboxService outboxService;

    private final KafkaTopicProperties kafkaTopicProperties;

    @Override
    public void execute(OrderSaga saga, Order order) {
        saga.setState(SAGA_STATE);

        SagaEvents.WarehouseReservationRequest warehouseEvent = SagaEvents.WarehouseReservationRequest.builder()
                .sagaId(saga.getSagaId())
                .orderId(saga.getOrderId())
                .userId(order.getUserId())
                .items(order.getPositions().stream()
                        .map(p -> SagaEvents.WarehouseReservationRequest.ReservationItem.builder()
                                .catalogItemId(p.getCatalogItemId())
                                .quantity(p.getQuantity())
                                .build())
                        .toList())
                .build();
        outboxService.saveEvent(
                EVENT_TYPE,
                saga.getSagaId().toString(),
                "Saga",
                warehouseEvent,
                kafkaTopicProperties.getRequestTopic(saga.getState()));

        log.info("Warehouse requested for order {} via saga {}", order.getId(), saga.getSagaId());
    }

    @Override
    public void compensate(OrderSaga saga, Order order, String reason) {

        SagaEvents.WarehouseReleaseEvent releaseEvent = SagaEvents.WarehouseReleaseEvent.builder()
                .sagaId(saga.getSagaId())
                .orderId(saga.getOrderId())
                .reason(reason)
                .build();
    }

    @Override
    public boolean canHandle(OrderSaga.SagaState state) {
        return state == SAGA_STATE;
    }

    @Override
    public OrderSaga.SagaState getHandledState() {
        return SAGA_STATE;
    }
}
