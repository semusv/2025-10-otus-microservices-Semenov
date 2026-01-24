package com.microservices.auth.config.properties;

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

    public String getAuthServiceUrl() {
        return services.get("auth-service").getUrl();
    }

    @Setter
    @Getter
    public static class ServiceConfig {
        private String url;

        private Map<String, String> endpoints = new HashMap<>();

        public String getEndpoint(String key) {
            return endpoints.get(key);
        }

        public String getFullUrl(String endpointKey) {
            String endpoint = endpoints.get(endpointKey);
            if (endpoint == null) {
                throw new IllegalArgumentException(String.format("Endpoint '%s' not found for service", endpointKey));
            }
            return url + endpoint;
        }
    }
}
