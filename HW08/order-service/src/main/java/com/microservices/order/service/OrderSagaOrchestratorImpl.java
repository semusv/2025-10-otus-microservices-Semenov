package com.microservices.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.order.kafka.SagaEvents.DeliveryRequestEvent;
import com.microservices.order.kafka.SagaEvents.DeliveryResponseEvent;
import com.microservices.order.kafka.SagaEvents.OrderCreatedEvent;
import com.microservices.order.kafka.SagaEvents.OrderFailedEvent;
import com.microservices.order.kafka.SagaEvents.PaymentRefundEvent;
import com.microservices.order.kafka.SagaEvents.PaymentRequestEvent;
import com.microservices.order.kafka.SagaEvents.PaymentResponseEvent;
import com.microservices.order.kafka.SagaEvents.WarehouseCancelEvent;
import com.microservices.order.kafka.SagaEvents.WarehouseReservationRequest;
import com.microservices.order.kafka.SagaEvents.WarehouseReservationResponse;
import com.microservices.order.model.EventType;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import com.microservices.order.repository.OrderRepository;
import com.microservices.order.repository.OrderSagaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("CheckStyle")
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestratorImpl implements OrderSagaOrchestrator {

    private final OrderSagaRepository sagaRepository;

    private final OrderRepository orderRepository;

    private final OutboxService outboxService;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    // Время задержки для проверки состояния саги
    @Value("${app.saga.delay_threshold: 5000}")
    private int delayThreshold;

    @Value("${app.saga.max_retries: 5}")
    private int maxRetries;

    public static class SagaException extends RuntimeException {
        public SagaException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Transactional
    @Override
    public void startOrderSaga(Order order) {
        try {
            // Создаем запись саги
            OrderSaga saga = new OrderSaga();
            saga.setSagaId(UUID.randomUUID());
            saga.setOrderId(order.getId());
            saga.setState(OrderSaga.SagaState.STARTED);
            sagaRepository.save(saga);

            log.info("Started saga {} for order {}", saga.getSagaId(), order.getId());

            // Сохраняем событие создания заказа
            OrderCreatedEvent orderEvent = OrderCreatedEvent.builder()
                    .eventId(UUID.randomUUID())
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .totalAmount(order.getPrice())
                    .createdAt(order.getCreatedAt())
                    .items(order.getPositions().stream()
                            .map(p -> OrderCreatedEvent.OrderItem.builder()
                                    .catalogItemId(p.getCatalogItemId())
                                    .quantity(p.getQuantity())
                                    .price(p.getPrice())
                                    .build())
                            .toList())
                    .build();

            outboxService.saveEvent(
                    EventType.ORDER_CREATED, order.getId().toString(), "Order", orderEvent, "order-events");

            // Переходим к оплате
            proceedToPayment(saga, order);

        } catch (Exception e) {
            log.error("Failed to start order saga for order {}", order.getId(), e);
            throw new SagaException("Failed to start order saga", e);
        }
    }

    private void proceedToPayment(OrderSaga saga, Order order) {
        saga.setState(OrderSaga.SagaState.PAYMENT_PROCESSING);
        sagaRepository.save(saga);

        PaymentRequestEvent paymentEvent = PaymentRequestEvent.builder()
                .sagaId(saga.getSagaId())
                .orderId(order.getId())
                .userId(order.getUserId())
                .amount(order.getPrice())
                .description("Payment for order #" + order.getId())
                .build();

        outboxService.saveEvent(
                EventType.PAYMENT_REQUESTED, saga.getSagaId().toString(), "Saga", paymentEvent, "payment-request");

        log.info("Payment requested for order {} via saga {}", order.getId(), saga.getSagaId());
    }

    @KafkaListener(topics = "payment-response", groupId = "order-service")
    @Transactional
    @Override
    public void handlePaymentResponse(String message) {
        try {
            PaymentResponseEvent event = objectMapper.readValue(message, PaymentResponseEvent.class);
            OrderSaga saga = sagaRepository
                    .findById(event.getSagaId())
                    .orElseThrow(() -> new RuntimeException("Saga not found: " + event.getSagaId()));

            log.info("Received payment response for saga {}: {}", saga.getSagaId(), event.isSuccess());

            if (event.isSuccess()) {
                saga.setState(OrderSaga.SagaState.PAYMENT_COMPLETED);
                sagaRepository.save(saga);

                // Платеж успешен -> резервируем товары
                reserveWarehouseItems(saga, event.getOrderId());
            } else {
                saga.setState(OrderSaga.SagaState.PAYMENT_FAILED);
                saga.setErrorMessage(event.getErrorMessage());
                sagaRepository.save(saga);

                triggerCompensation(saga, "Payment failed: " + event.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Failed to process payment response: {}", message, e);
        }
    }

    private void reserveWarehouseItems(OrderSaga saga, UUID orderId) {
        saga.setState(OrderSaga.SagaState.WAREHOUSE_RESERVING);
        sagaRepository.save(saga);

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        WarehouseReservationRequest request = WarehouseReservationRequest.builder()
                .sagaId(saga.getSagaId())
                .orderId(orderId)
                .userId(order.getUserId())
                .items(order.getPositions().stream()
                        .map(p -> WarehouseReservationRequest.ReservationItem.builder()
                                .catalogItemId(p.getCatalogItemId())
                                .quantity(p.getQuantity())
                                .build())
                        .toList())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        outboxService.saveEvent(
                EventType.WAREHOUSE_REQUESTED,
                saga.getSagaId().toString(),
                "Saga",
                request,
                "warehouse-reservation-request");

        log.info("Warehouse reservation requested for order {}", orderId);
    }

    @KafkaListener(topics = "warehouse-reservation-response", groupId = "order-service")
    @Transactional
    @Override
    public void handleWarehouseResponse(String message) {
        try {
            WarehouseReservationResponse event = objectMapper.readValue(message, WarehouseReservationResponse.class);
            OrderSaga saga = sagaRepository
                    .findById(event.getSagaId())
                    .orElseThrow(() -> new RuntimeException("Saga not found: " + event.getSagaId()));

            log.info("Received warehouse response for saga {}: {}", saga.getSagaId(), event.isSuccess());

            if (event.isSuccess()) {
                saga.setState(OrderSaga.SagaState.WAREHOUSE_RESERVED);
                sagaRepository.save(saga);

                // Товары зарезервированы -> планируем доставку
                scheduleDelivery(saga, event.getOrderId());
            } else {
                saga.setState(OrderSaga.SagaState.WAREHOUSE_FAILED);
                saga.setErrorMessage(event.getErrorMessage());
                sagaRepository.save(saga);

                triggerCompensation(saga, "Warehouse reservation failed: " + event.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Failed to process warehouse response: {}", message, e);
        }
    }

    private void scheduleDelivery(OrderSaga saga, UUID orderId) {
        saga.setState(OrderSaga.SagaState.DELIVERY_SCHEDULING);
        sagaRepository.save(saga);

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        // В реальном приложении здесь был бы запрос адреса доставки из профиля
        DeliveryRequestEvent request = DeliveryRequestEvent.builder()
                .sagaId(saga.getSagaId())
                .orderId(orderId)
                .userId(order.getUserId())
                .deliveryAddress("User address from profile")
                .deliveryDate(LocalDateTime.now().plusDays(1))
                .items(order.getPositions().stream()
                        .map(p -> DeliveryRequestEvent.DeliveryItem.builder()
                                .productId(p.getCatalogItemId()) // В реальности здесь productId
                                .productName("Product name from catalog") // Из каталога
                                .quantity(p.getQuantity())
                                .build())
                        .toList())
                .build();

        outboxService.saveEvent(
                EventType.DELIVERY_REQUESTED, saga.getSagaId().toString(), "Saga", request, "delivery-request");

        log.info("Delivery requested for order {}", orderId);
    }

    @KafkaListener(topics = "delivery-response", groupId = "order-service")
    @Transactional
    @Override
    public void handleDeliveryResponse(String message) {
        try {
            DeliveryResponseEvent event = objectMapper.readValue(message, DeliveryResponseEvent.class);
            OrderSaga saga = sagaRepository
                    .findById(event.getSagaId())
                    .orElseThrow(() -> new RuntimeException("Saga not found: " + event.getSagaId()));

            log.info("Received delivery response for saga {}: {}", saga.getSagaId(), event.isSuccess());

            if (event.isSuccess()) {
                // Сага успешно завершена
                completeSaga(saga);
            } else {
                saga.setState(OrderSaga.SagaState.DELIVERY_FAILED);
                saga.setErrorMessage(event.getErrorMessage());
                sagaRepository.save(saga);

                triggerCompensation(saga, "Delivery failed: " + event.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Failed to process delivery response: {}", message, e);
        }
    }

    private void completeSaga(OrderSaga saga) {
        saga.setState(OrderSaga.SagaState.COMPLETED);
        sagaRepository.save(saga);

        // Обновляем статус заказа
        Order order = orderRepository
                .findById(saga.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));
        order.setStatus(Order.Status.PAID);
        orderRepository.save(order);

        log.info("Saga {} completed successfully for order {}", saga.getSagaId(), saga.getOrderId());
    }

    private void triggerCompensation(OrderSaga saga, String reason) {
        saga.setState(OrderSaga.SagaState.COMPENSATING);
        sagaRepository.save(saga);

        log.info("Starting compensation for saga {}: {}", saga.getSagaId(), reason);

        // Отправляем компенсирующие события в обратном порядке
        Order order = orderRepository
                .findById(saga.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + saga.getOrderId()));

        // 1. Отменяем доставку если была запланирована
        if (saga.getState().ordinal() >= OrderSaga.SagaState.DELIVERY_SCHEDULING.ordinal()) {
            // Здесь отправляем событие отмены доставки
        }

        // 2. Отменяем резервирование склада если было
        if (saga.getState().ordinal() >= OrderSaga.SagaState.WAREHOUSE_RESERVED.ordinal()) {
            WarehouseCancelEvent cancelEvent = WarehouseCancelEvent.builder()
                    .sagaId(saga.getSagaId())
                    .orderId(saga.getOrderId())
                    .reason(reason)
                    .build();

            outboxService.saveEvent(
                    EventType.WAREHOUSE_CANCELED, saga.getSagaId().toString(), "Saga", cancelEvent, "warehouse-cancel");
        }

        // 3. Возвращаем платеж если был
        if (saga.getState().ordinal() >= OrderSaga.SagaState.PAYMENT_COMPLETED.ordinal()) {
            PaymentRefundEvent refundEvent = PaymentRefundEvent.builder()
                    .sagaId(saga.getSagaId())
                    .orderId(saga.getOrderId())
                    .amount(order.getPrice())
                    .reason(reason)
                    .build();

            outboxService.saveEvent(
                    EventType.PAYMENT_REFUNDED, saga.getSagaId().toString(), "Saga", refundEvent, "payment-refund");
        }

        // 4. Обновляем статус заказа
        order.setStatus(Order.Status.FAILED);
        orderRepository.save(order);

        saga.setState(OrderSaga.SagaState.FAILED);
        saga.setErrorMessage(reason);
        sagaRepository.save(saga);

        // Отправляем событие о неудачном заказе
        OrderFailedEvent failedEvent = OrderFailedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .reason(reason)
                .build();

        outboxService.saveEvent(EventType.ORDER_FAILED, order.getId().toString(), "Order", failedEvent, "order-failed");

        log.info("Compensation completed for saga {}", saga.getSagaId());
    }

    @Scheduled(fixedDelayString = "${app.saga.polling_delay: 10000}")
    @Transactional
    @Override
    public void recoverStuckSagas() {
        List<OrderSaga> stuckSagas = findStuckSagas();
        if (stuckSagas.isEmpty()) {
            return;
        }
        log.warn("Found {} stuck sagas for recovery", stuckSagas.size());
        for (OrderSaga saga : stuckSagas) {
            recoverSaga(saga);
        }
    }

    private List<OrderSaga> findStuckSagas() {
        List<OrderSaga.SagaState> stuckStates = List.of(
                OrderSaga.SagaState.PAYMENT_PROCESSING,
                OrderSaga.SagaState.WAREHOUSE_RESERVING,
                OrderSaga.SagaState.DELIVERY_SCHEDULING);
        return sagaRepository.findStuckSagas(stuckStates).stream()
                .filter(saga -> saga.getUpdatedAt().isBefore(LocalDateTime.now().minusSeconds(delayThreshold / 1000)))
                .filter(saga -> saga.getRetryCount() < maxRetries)
                .toList();
    }

    private void recoverSaga(OrderSaga saga) {
        log.warn(
                "Recovering saga {} in state {}, retry {}/3",
                saga.getSagaId(),
                saga.getState(),
                saga.getRetryCount() + 1);
        saga.setRetryCount(saga.getRetryCount() + 1);
        try {
            if (saga.getRetryCount() >= 3) {
                handleMaxRetriesExceeded(saga);
            } else {
                retryCurrentStep(saga);
                sagaRepository.save(saga);
                log.info("Recovery initiated for saga {} (attempt {})", saga.getSagaId(), saga.getRetryCount());
            }
        } catch (Exception e) {
            log.error("Failed to recover saga {}", saga.getSagaId(), e);
            if (saga.getRetryCount() >= 3) {
                triggerCompensation(saga, "Automatic recovery failed: " + e.getMessage());
            }
        }
    }

    private void handleMaxRetriesExceeded(OrderSaga saga) {
        String errorMsg =
                String.format("Maximum retry limit (%d) reached for state %s", saga.getRetryCount(), saga.getState());

        log.error("Compensating saga {}: {}", saga.getSagaId(), errorMsg);
        triggerCompensation(saga, errorMsg);
    }

    private void retryCurrentStep(OrderSaga saga) {
        Order order = orderRepository
                .findById(saga.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found for saga: " + saga.getSagaId()));

        switch (saga.getState()) {
            case PAYMENT_PROCESSING:
                proceedToPayment(saga, order);
                break;

            case WAREHOUSE_RESERVING:
                reserveWarehouseItems(saga, order.getId());
                break;

            case DELIVERY_SCHEDULING:
                scheduleDelivery(saga, order.getId());
                break;

            default:
                log.error("Cannot recover saga {} in state {}", saga.getSagaId(), saga.getState());
                break;
        }
    }
}
