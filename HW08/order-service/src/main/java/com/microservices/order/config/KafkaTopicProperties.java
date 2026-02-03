package com.microservices.order.config;

import com.microservices.order.model.OrderSaga;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.kafka.topics")
@Data
public class KafkaTopicProperties {

    // Request topics
    private String paymentRequest;

    private String paymentResponse;

    private String warehouseReservationRequest;

    private String warehouseReservationResponse;

    private String deliveryRequest;

    private String deliveryResponse;

    // Compensation topics

    private String paymentCompensationRequest;

    private String paymentCompensationResponse;

    private String warehouseReservationCompensationRequest;

    private String warehouseReservationCompensationResponse;

    private String deliveryCompensationRequest;

    private String deliveryCompensationResponse;

    // Отмена заказа
    private String orderCancellingRequest;

    // Геттер для топика по состоянию
    public String getRequestTopic(OrderSaga.SagaState state) {
        return switch (state) {
            case PAYMENT_PROCESSING -> paymentRequest;
            case WAREHOUSE_RESERVING -> warehouseReservationRequest;
            case DELIVERY_SCHEDULING -> deliveryRequest;
            default -> throw new IllegalArgumentException("No topic for state: " + state);
        };
    }
}
