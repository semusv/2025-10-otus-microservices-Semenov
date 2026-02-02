package com.microservices.order.service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.order.config.KafkaTopicProperties;
import com.microservices.order.kafka.SagaEvents.CompensationResponseEvent;
import com.microservices.order.kafka.SagaEvents.DeliveryResponseEvent;
import com.microservices.order.kafka.SagaEvents.OrderFailedEvent;
import com.microservices.order.kafka.SagaEvents.PaymentResponseEvent;
import com.microservices.order.kafka.SagaEvents.WarehouseReservationResponse;
import com.microservices.order.model.EventType;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.repository.SagaRepository;
import com.microservices.order.service.OutboxService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestratorImpl implements OrderSagaOrchestrator {

    private final SagaRepository sagaRepository;

    private final OrderRepository orderRepository;

    private final SagaStateMachine stateMachine;

    private final SagaRecoveryService recoveryService;

    private final SagaCompensationExecutor compensationExecutor;

    private final OutboxService outboxService;

    private final ObjectMapper objectMapper;

    private final KafkaTopicProperties kafkaTopicProperties;

    @Transactional
    @Override
    public void startOrderSaga(Order order) {
        try {
            OrderSaga saga = createSaga(order);
            log.info("Started saga {} for order {}", saga.getSagaId(), order.getId());
            // Используем метод с автоматическим переходом
            stateMachine.process(saga, order);
            // Сохраняем изменения
            sagaRepository.save(saga);

        } catch (Exception e) {
            log.error("Failed to start order saga for order {}", order.getId(), e);
            throw new SagaException("Failed to start order saga", e);
        }
    }

    private OrderSaga createSaga(Order order) {
        OrderSaga saga = new OrderSaga();
        saga.setSagaId(UUID.randomUUID());
        saga.setOrderId(order.getId());
        saga.setState(OrderSaga.SagaState.STARTED);
        return sagaRepository.save(saga);
    }

    @KafkaListener(
            id = "paymentListener",
            topics = "#{@kafkaTopicProperties.getPaymentResponse()}",
            groupId = "order-service")
    @Transactional
    public void handlePaymentResponse(String message) {
        try {
            PaymentResponseEvent event = objectMapper.readValue(message, PaymentResponseEvent.class);
            OrderSaga saga = getSagaFromEvent(event);

            if (event.isSuccess()) {
                // Выполняем следующий шаг и переходим к новому состоянию
                Order order = getOrder(saga.getOrderId());
                order.setStatus(Order.Status.PAID);
                orderRepository.save(order);
                // Фиксируем факт события
                saga.markPaymentExecuted();
                saga.setState(OrderSaga.SagaState.PAYMENT_COMPLETED);
                sagaRepository.save(saga);

                stateMachine.process(saga, order);
                sagaRepository.save(saga);
                log.info("Payment successful, saga {} moved to warehouse reservation", saga.getSagaId());

            } else {
                handleFailure(saga, OrderSaga.SagaState.PAYMENT_FAILED, "Payment failed: " + event.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Failed to process payment response: {}", message, e);
        }
    }

    // названиетопика из конфигурации
    @KafkaListener(
            id = "warehouseListener",
            topics = "#{@kafkaTopicProperties.getWarehouseResponse()}",
            groupId = "order-service")
    @Transactional
    public void handleWarehouseResponse(String message) {
        try {
            WarehouseReservationResponse event = objectMapper.readValue(message, WarehouseReservationResponse.class);
            OrderSaga saga = getSagaFromEvent(event);
            if (event.isSuccess()) {
                Order order = getOrder(saga.getOrderId());
                order.setStatus(Order.Status.RESERVED);
                orderRepository.save(order);
                // Фиксируем факт события
                saga.markWarehouseExecuted();
                saga.setState(OrderSaga.SagaState.WAREHOUSE_RESERVED);
                sagaRepository.save(saga);

                stateMachine.process(saga, order);
                sagaRepository.save(saga);
                log.info("Warehouse reservation successful, saga {} moved to delivery scheduling", saga.getSagaId());
            } else {
                handleFailure(
                        saga,
                        OrderSaga.SagaState.COMPENSATING,
                        "Warehouse reservation failed: " + event.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Failed to process warehouse response: {}", message, e);
        }
    }

    @KafkaListener(
            id = "deliveryListener",
            topics = "#{@kafkaTopicProperties.getDeliveryResponse()}",
            groupId = "order-service")
    @Transactional
    public void handleDeliveryResponse(String message) {
        try {
            DeliveryResponseEvent event = objectMapper.readValue(message, DeliveryResponseEvent.class);
            OrderSaga saga = getSagaFromEvent(event);

            if (event.isSuccess()) {
                Order order = getOrder(saga.getOrderId());
                order.setStatus(Order.Status.DELIVERED);
                orderRepository.save(order);
                // Фиксируем факт события
                saga.markDeliveryExecuted();
                saga.setState(OrderSaga.SagaState.DELIVERY_SCHEDULED);
                sagaRepository.save(saga);

                stateMachine.process(saga, order);
                sagaRepository.save(saga);
                log.info("Delivery successful, saga {} moved to completed", saga.getSagaId());
            } else {
                handleFailure(saga, OrderSaga.SagaState.DELIVERY_FAILED, "Delivery failed: " + event.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Failed to process delivery response: {}", message, e);
        }
    }

    @KafkaListener(
            id = "paymentCompensationListener",
            topics = "#{@kafkaTopicProperties.getPaymentCompensateResponse()}",
            groupId = "order-service")
    @Transactional
    public void handlePaymentCompensateResponse(String message) {
        try {
            CompensationResponseEvent event = objectMapper.readValue(message, CompensationResponseEvent.class);
            OrderSaga saga = getSagaFromEvent(event);

            if (event.isSuccess()) {
                Order order = getOrder(saga.getOrderId());
                compensationExecutor.compensatePayment(saga);
                sagaRepository.save(saga);
                if (saga.getState() == OrderSaga.SagaState.COMPENSATED) {
                    markOrderAsCancelled(order);
                }
                log.info("Payment compensation successful, saga {} moved to {}", saga.getSagaId(), saga.getState());
            } else {
                log.warn("Payment compensation failed: {}", saga.getSagaId());
            }
        } catch (Exception e) {
            log.error("Failed to process payment compensation response: {}", message, e);
        }
    }

    @KafkaListener(
            id = "warehouseCompensationListener",
            topics = "#{@kafkaTopicProperties.getWarehouseCompensateResponse()}",
            groupId = "order-service")
    @Transactional
    public void handleWarehouseCompensateResponse(String message) {
        try {
            CompensationResponseEvent event = objectMapper.readValue(message, CompensationResponseEvent.class);
            OrderSaga saga = getSagaFromEvent(event);

            if (event.isSuccess()) {
                Order order = getOrder(saga.getOrderId());
                compensationExecutor.compensateWarehouse(saga);
                sagaRepository.save(saga);
                if (saga.getState() == OrderSaga.SagaState.COMPENSATED) {
                    markOrderAsCancelled(order);
                }
                log.info("Warehouse compensation successful, saga {} moved to {}", saga.getSagaId(), saga.getState());
            } else {
                log.warn("Warehouse compensation failed: {}", saga.getSagaId());
            }
        } catch (Exception e) {
            log.error("Failed to process warehouse compensation response: {}", message, e);
        }
    }

    @KafkaListener(
            id = "deliveryCompensationListener",
            topics = "#{@kafkaTopicProperties.getDeliveryCompensateResponse()}",
            groupId = "order-service")
    @Transactional
    public void handleDeliveryCompensateResponse(String message) {
        try {
            CompensationResponseEvent event = objectMapper.readValue(message, CompensationResponseEvent.class);
            OrderSaga saga = getSagaFromEvent(event);

            if (event.isSuccess()) {
                Order order = getOrder(saga.getOrderId());
                compensationExecutor.compensateDelivery(saga);
                sagaRepository.save(saga);
                if (saga.getState() == OrderSaga.SagaState.COMPENSATED) {
                    markOrderAsCancelled(order);
                }
                log.info("Delivery compensation successful, saga {} moved to {}", saga.getSagaId(), saga.getState());
            } else {
                log.warn("Delivery compensation failed: {}", saga.getSagaId());
            }
        } catch (Exception e) {
            log.error("Failed to process delivery compensation response: {}", message, e);
        }
    }

    private void handleFailure(OrderSaga saga, OrderSaga.SagaState failedState, String reason) {
        saga.setState(failedState);
        saga.setErrorMessage(reason);
        sagaRepository.save(saga);

        compensateSaga(saga, reason);
    }

    private void compensateSaga(OrderSaga saga, String reason) {
        saga.setState(OrderSaga.SagaState.COMPENSATING);
        saga.setErrorMessage(reason);
        sagaRepository.save(saga);

        log.info("Starting compensation for saga {}: {}", saga.getSagaId(), reason);

        Order order = getOrder(saga.getOrderId());
        compensationExecutor.executeCompensation(saga, order, reason);

        markOrderAsCancelling(order, reason);
    }

    private OrderSaga getSagaFromEvent(Object event) {
        try {
            Method getSagaId = event.getClass().getMethod("getSagaId");
            UUID sagaId = (UUID) getSagaId.invoke(event);
            return sagaRepository.findById(sagaId).orElseThrow(() -> {
                log.error("Saga not found: {}", sagaId);
                return new RuntimeException("Saga not found: " + sagaId);
            });
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new SagaException(e.getMessage(), e);
        }
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    private void markOrderAsCancelling(Order order, String reason) {
        order.setStatus(Order.Status.CANCELLING);
        orderRepository.save(order);

        OrderFailedEvent failedEvent = OrderFailedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .reason(reason)
                .build();

        outboxService.saveEvent(
                EventType.ORDER_CANCELLING,
                order.getId().toString(),
                "Order",
                failedEvent,
                kafkaTopicProperties.getOrderCancellingRequest());
    }

    private void markOrderAsCancelled(Order order) {
        order.setStatus(Order.Status.CANCELLING);
        orderRepository.save(order);

        OrderFailedEvent failedEvent = OrderFailedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .reason("Saga failed")
                .build();

        outboxService.saveEvent(
                EventType.ORDER_CANCELED,
                order.getId().toString(),
                "Order",
                failedEvent,
                kafkaTopicProperties.getOrderCancellingRequest());
    }

    @Scheduled(fixedDelayString = "${app.saga.polling_delay: 30000}")
    @Transactional
    @Override
    public void recoverStuckSagas() {
        recoveryService.recoverStuckSagas(stateMachine);
    }
}
