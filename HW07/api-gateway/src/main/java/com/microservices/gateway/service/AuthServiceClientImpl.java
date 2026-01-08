package com.microservices.gateway.service;

import com.microservices.gateway.component.resolver.EndpointResolver;
import com.microservices.gateway.config.properties.SecurityProperties;
import com.microservices.gateway.dto.TokenValidationResponse;
import com.microservices.gateway.dto.ValidateTokenRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthServiceClientImpl implements AuthServiceClient {

    @Value("${spring.application.name}")
    private String serviceName;

    private final SecurityProperties securityProperties;

    private final EndpointResolver endpointResolver;

    private final WebClient webClient;

    /**
     * Валидация токена через auth-service
     * С кэшированием для уменьшения нагрузки
     */
    @Cacheable(value = "tokenValidation", key = "#token", unless = "#result.valid == false")
    @Override
    public Mono<TokenValidationResponse> validateToken(String token) {
        log.debug("Validating token via auth-service: {}", maskToken(token));

        return Mono.deferContextual(ctx -> buildWebClientRequest(token, ctx))
                .onErrorResume(this::handleValidationError);
    }

    private Mono<TokenValidationResponse> buildWebClientRequest(String token, ContextView ctx) {
        String requestId = ctx.getOrDefault(securityProperties.getRequestIdHeader(), "");
        ValidateTokenRequest request = new ValidateTokenRequest(token);

        return webClient
                .post()
                .uri(endpointResolver.getAuthServiceUrl("validate"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .headers(headers -> addHeaders(headers, requestId))
                .retrieve()
                .bodyToMono(TokenValidationResponse.class)
                .doOnNext(response -> logValidationResult(response, requestId));
    }

    private void addHeaders(HttpHeaders headers, String requestId) {
        headers.add(securityProperties.getServiceSecretHeader(), securityProperties.getSecret());
        headers.add(securityProperties.getServiceNameHeader(), serviceName);
        headers.add(securityProperties.getRequestIdHeader(), requestId);
    }

    private void logValidationResult(TokenValidationResponse response, String requestId) {
        if (response.isValid()) {
            log.debug("Token valid for user: {} [RequestId: {}", response.getUsername(), requestId);
        } else {
            log.warn("Token invalid: {} [RequestId: {}", response.getError(), requestId);
        }
    }

    private Mono<TokenValidationResponse> handleValidationError(Throwable e) {
        log.error("Error validating token: {}", e.getMessage());
        return Mono.just(createErrorResponse());
    }

    private TokenValidationResponse createErrorResponse() {
        TokenValidationResponse response = new TokenValidationResponse();
        response.setValid(false);
        response.setError("Auth service unavailable");
        return response;
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 7) + "..." + token.substring(token.length() - 3);
    }
}
