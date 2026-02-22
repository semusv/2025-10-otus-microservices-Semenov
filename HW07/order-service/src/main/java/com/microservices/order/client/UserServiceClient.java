package com.microservices.order.client;

import com.microservices.order.config.properties.SecurityProperties;
import com.microservices.order.resolver.EndpointResolver;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.vvsem.shared.dto.shared_api_dto.UserProfileResponse;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {
    private final RestClient userServiceRestClient;

    private final SecurityProperties securityProperties;

    private final EndpointResolver endpointResolver;

    @Value("${spring.application.name}")
    private String serviceName;

    public UserProfileResponse fetchMyProfile(UUID userId) {
        log.info("Calling user-service to get  my profile for user: {}", userId);

        try {
            UserProfileResponse userProfileResponse = userServiceRestClient
                    .get()
                    .uri(endpointResolver.getUserServiceUrl("me"))
                    .header(securityProperties.getUserIdHeader(), userId.toString())
                    .header(securityProperties.getRequestIdHeader(), MDC.get(securityProperties.getRequestIdHeader()))
                    .header(securityProperties.getServiceNameHeader(), serviceName)
                    .header(securityProperties.getServiceSecretHeader(), securityProperties.getSecret())
                    .retrieve()
                    .body(UserProfileResponse.class);
            log.info("Successfully get user profile for: {}", userId);
            return userProfileResponse;
        } catch (Exception e) {
            log.error("Failed to get user profile for {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to call user-service: " + e.getMessage(), e);
        }
    }

    public String checkUserServiceHealth() {
        return userServiceRestClient.get().uri("/actuator/health").retrieve().body(String.class);
    }
}
