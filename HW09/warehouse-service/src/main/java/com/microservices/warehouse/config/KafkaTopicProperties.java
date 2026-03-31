package com.microservices.warehouse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.kafka.topics")
@Data
public class KafkaTopicProperties {

    // Request topics
    private String warehouseReservationRequest;

    private String warehouseReservationResponse;

    // Compensation topics

    private String warehouseReservationCompensationRequest;

    private String warehouseReservationCompensationResponse;

    // Отмена заказа
    private String orderCancellingRequest;
}
