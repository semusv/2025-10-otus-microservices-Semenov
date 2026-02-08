package com.microservices.order.service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.order.kafka.SagaEvents;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import com.microservices.order.repository.SagaRepository;
import com.microservices.order.service.saga.handlers.CompensationResponseHandler;
import com.microservices.order.service.saga.handlers.DeliveryResponseHandler;
import com.microservices.order.service.saga.handlers.PaymentResponseHandler;
import com.microservices.order.service.saga.handlers.WarehouseResponseHandler;
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

    private final SagaStateMachine stateMachine;

    private final SagaRecoveryService recoveryService;

    private final ObjectMapper objectMapper;

    private final PaymentResponseHandler paymentResponseHandler;

    private final WarehouseResponseHandler warehouseResponseHandler;

    private final DeliveryResponseHandler deliveryResponseHandler;

    private final CompensationResponseHandler compensationResponseHandler;

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
        handleMessage(message, SagaEvents.PaymentResponseEvent.class, paymentResponseHandler::processPaymentResponse);
    }

    // названиетопика из конфигурации
    @KafkaListener(
            id = "warehouseListener",
            topics = "#{@kafkaTopicProperties.getWarehouseReservationResponse()}",
            groupId = "order-service")
    @Transactional
    public void handleWarehouseResponse(String message) {
        handleMessage(
                message,
                SagaEvents.WarehouseReservationResponseEvent.class,
                warehouseResponseHandler::processWarehouseResponse);
    }

    @KafkaListener(
            id = "deliveryListener",
            topics = "#{@kafkaTopicProperties.getDeliveryReservationResponse()}",
            groupId = "order-service")
    @Transactional
    public void handleDeliveryResponse(String message) {
        handleMessage(
                message, SagaEvents.DeliveryResponseEvent.class, deliveryResponseHandler::processDeliveryResponse);
    }

    @KafkaListener(
            id = "paymentCompensationListener",
            topics = "#{@kafkaTopicProperties.getPaymentCompensationResponse()}",
            groupId = "order-service")
    @Transactional
    public void handlePaymentCompensationResponse(String message) {
        handleMessage(
                message,
                SagaEvents.CompensationResponseEvent.class,
                compensationResponseHandler::processPaymentCompensationResponse);
    }

    @KafkaListener(
            id = "warehouseCompensationListener",
            topics = "#{@kafkaTopicProperties.getWarehouseReservationCompensationResponse()}",
            groupId = "order-service")
    @Transactional
    public void handleWarehouseCompensationResponse(String message) {
        handleMessage(
                message,
                SagaEvents.CompensationResponseEvent.class,
                compensationResponseHandler::processWarehouseCompensationResponse);
    }

    @KafkaListener(
            id = "deliveryCompensationListener",
            topics = "#{@kafkaTopicProperties.getDeliveryReservationCompensationResponse()}",
            groupId = "order-service")
    @Transactional
    public void handleDeliveryCompensationResponse(String message) {
        handleMessage(
                message,
                SagaEvents.CompensationResponseEvent.class,
                compensationResponseHandler::processDeliveryCompensationResponse);
    }

    private OrderSaga getSagaFromEvent(Object event) {
        try {
            Method getSagaId = event.getClass().getMethod("getSagaId");
            UUID sagaId = (UUID) getSagaId.invoke(event);
            return sagaRepository.findBySagaId(sagaId).orElseThrow(() -> {
                log.error("Saga not found: {}", sagaId);
                return new RuntimeException("Saga not found: " + sagaId);
            });
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new SagaException(e.getMessage(), e);
        }
    }

    private <T> void handleMessage(
            String message, Class<T> eventClass, java.util.function.BiConsumer<T, OrderSaga> processor) {
        try {
            T event = objectMapper.readValue(message, eventClass);
            OrderSaga saga = getSagaFromEvent(event);
            processor.accept(event, saga);
        } catch (Exception e) {
            log.error("Failed to process {} response: {}", eventClass.getSimpleName(), message, e);
        }
    }

    @Scheduled(fixedDelayString = "${app.saga.polling_delay: 30000}")
    @Transactional
    @Override
    public void recoverStuckSagas() {
        recoveryService.recoverStuckSagas(stateMachine);
        recoveryService.compensateFailedSagas(stateMachine);
    }
}
