package com.microservices.delivery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.kafka.topics")
@Data
public class KafkaTopicProperties {

    // Request topics
    private String deliveryReservationRequest;

    private String deliveryReservationResponse;

    // Compensation topics

    private String deliveryReservationCompensationRequest;

    private String deliveryReservationCompensationResponse;

    // Отмена заказа
    private String orderCancellingRequest;
}
