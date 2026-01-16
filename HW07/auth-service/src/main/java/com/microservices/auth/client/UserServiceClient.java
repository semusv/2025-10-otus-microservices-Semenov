package com.microservices.auth.client;

import com.microservices.auth.comonents.resolver.EndpointResolver;
import com.microservices.auth.config.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.vvsem.shared.dto.shared_api_dto.UserCreateProfileRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {
    private final RestClient userServiceRestClient;

    private final SecurityProperties securityProperties;

    private final EndpointResolver endpointResolver;

    public void createUserProfile(UserCreateProfileRequest request) {
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
