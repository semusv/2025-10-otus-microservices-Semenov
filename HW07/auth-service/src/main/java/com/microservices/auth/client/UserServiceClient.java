package com.microservices.auth.client;

import com.microservices.auth.comonents.resolver.EndpointResolver;
import com.microservices.auth.config.properties.SecurityProperties;
import com.microservices.auth.dto.auth.ProfileCreationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {
    private final RestClient userServiceRestClient;

    private final SecurityProperties securityProperties;

    private final EndpointResolver endpointResolver;

    @Value("${spring.application.name}")
    private String serviceName;

    public void createUserProfile(ProfileCreationRequest request) {
        log.info("Calling user-service to create profile for user: {}", request.getUserId());

        try {
            userServiceRestClient
                    .post()
                    .uri(endpointResolver.getUserServiceUrl("profile"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                            securityProperties.getUserIdHeader(),
                            request.getUserId().toString())
                    .header(securityProperties.getRequestIdHeader(), MDC.get(securityProperties.getRequestIdHeader()))
                    .header(securityProperties.getServiceNameHeader(), serviceName)
                    .header(securityProperties.getServiceSecretHeader(), securityProperties.getSecret())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully created user profile for: {}", request.getUserId());

        } catch (Exception e) {
            log.error("Failed to create user profile for {}: {}", request.getUserId(), e.getMessage());
            throw new RuntimeException("Failed to call user-service: " + e.getMessage(), e);
        }
    }

    public String checkUserServiceHealth() {
        return userServiceRestClient.get().uri("/actuator/health").retrieve().body(String.class);
    }
}
