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

    private String warehouseRequest;

    private String warehouseResponse;

    private String deliveryRequest;

    private String deliveryResponse;

    // Compensation topics

    private String paymentCompensateRequest;

    private String warehouseCompensateRequest;

    private String deliveryCompensateRequest;

    private String paymentCompensateResponse;

    private String warehouseCompensateResponse;

    private String deliveryCompensateResponse;

    // Отмена заказа
    private String orderCancellingRequest;

    // Геттер для топика по состоянию
    public String getRequestTopic(OrderSaga.SagaState state) {
        return switch (state) {
            case PAYMENT_PROCESSING -> paymentRequest;
            case WAREHOUSE_RESERVING -> warehouseRequest;
            case DELIVERY_SCHEDULING -> deliveryRequest;
            default -> throw new IllegalArgumentException("No topic for state: " + state);
        };
    }
}
