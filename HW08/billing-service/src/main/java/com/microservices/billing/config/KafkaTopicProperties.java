package com.microservices.billing.config;

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

    // Compensation topics
    private String paymentCompensateRequest;

    private String paymentCompensateResponse;

    // User topics
    private String userCreated;
}
