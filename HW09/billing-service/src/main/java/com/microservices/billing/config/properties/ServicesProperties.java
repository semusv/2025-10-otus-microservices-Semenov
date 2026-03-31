package com.microservices.billing.config.properties;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
@Order(1)
public class ServicesProperties {

    private Map<String, ServiceConfig> services = new HashMap<>();

    public String getUserServiceUrl() {
        return services.get("user-service").getUrl();
    }

    public String getBillingServiceUrl() {
        return services.get("billing-service").getUrl();
    }

    @Setter
    @Getter
    public static class ServiceConfig {
        private String url;

        private Map<String, String> endpoints = new HashMap<>();
    }
}
