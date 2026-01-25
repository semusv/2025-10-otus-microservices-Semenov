package com.microservices.order.resolver;

import com.microservices.order.config.properties.ServicesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EndpointResolver {

    private final ServicesProperties servicesProperties;

    private String resolveUrl(String serviceName, String endpointKey) {
        ServicesProperties.ServiceConfig service =
                servicesProperties.getServices().get(serviceName);
        if (service == null) {
            throw new IllegalArgumentException("Service not found: " + serviceName);
        }
        return service.getFullUrl(endpointKey);
    }

    // Методы для конкретных сервисов
    public String getUserServiceUrl(String endpointKey) {
        return resolveUrl("user-service", endpointKey);
    }

    public String getBillingServiceUrl(String endpointKey) {
        return resolveUrl("billing-service", endpointKey);
    }
}
