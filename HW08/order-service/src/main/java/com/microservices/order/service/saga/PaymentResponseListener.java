package com.microservices.order.service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.order.kafka.SagaEvents;
import com.microservices.order.model.Order;
import com.microservices.order.model.OrderSaga;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentResponseListener {

    private final ObjectMapper objectMapper;

    private final OrderSagaOrchestratorImpl orderSagaOrchestrator;

    @KafkaListener(
            id = "paymentListener",
            topics = "#{@kafkaTopicProperties.getPaymentResponse()}",
            groupId = "order-service")
    @Transactional
    public void handlePaymentResponse(String message) {
        try {
            SagaEvents.PaymentResponseEvent event = objectMapper.readValue(message, SagaEvents.PaymentResponseEvent.class);
            OrderSaga saga = orderSagaOrchestrator.getSagaFromEvent(event);

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
}
