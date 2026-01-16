package com.microservices.gateway.filter;

import com.microservices.gateway.config.properties.SecurityProperties;
import com.microservices.gateway.service.AuthServiceClient;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class JwtValidationFilterFactory extends AbstractGatewayFilterFactory<JwtValidationFilterFactory.Config> {
    // Имя фильтра для использования в конфигурации YAML
    public static final String NAME_FILTER = "JwtValidationFilter";

    private final AuthServiceClient authServiceClient;

    private final SecurityProperties securityProperties;

    public JwtValidationFilterFactory(AuthServiceClient authServiceClient, SecurityProperties securityProperties) {
        super(Config.class);
        this.authServiceClient = authServiceClient;
        this.securityProperties = securityProperties;
    }

    @Override
    public GatewayFilter apply(JwtValidationFilterFactory.Config config) {
        return (exchange, chain) -> {
            log.debug(
                    "JWT Validation Filter processing request: {}",
                    exchange.getRequest().getPath());
            // Извлекаем токен
            String token = extractToken(exchange.getRequest().getHeaders());
            if (token == null) {
                log.warn(
                        "No JWT token found for request: {}",
                        exchange.getRequest().getPath());
                return unauthorized(exchange, "Missing authentication token");
            }

            return validateToken(exchange, chain, token);
        };
    }

    @Override
    public String name() {
        return NAME_FILTER;
    }

    private Mono<Void> validateToken(ServerWebExchange exchange, GatewayFilterChain chain, String token) {
        // Валидируем токен через auth-service
        return authServiceClient.validateToken(token).flatMap(validationResponse -> {
            if (authServiceClient.checkValidAccessToken(
                    validationResponse.getTokenType(), Boolean.TRUE.equals(validationResponse.getValid()))) {
                // Токен валиден, добавляем заголовки
                log.debug("Token valid, adding headers for user: {}", validationResponse.getUsername());
                return chain.filter(exchange.mutate()
                        .request(builder -> builder.header(
                                        securityProperties.getUserIdHeader(), validationResponse.getUserId())
                                .header(
                                        securityProperties.getRolesHeader(),
                                        validationResponse.getRoles().toString())
                                .build())
                        .build());
            } else {
                // Токен невалиден
                log.warn("Token validation failed: {}", validationResponse.getError());
                return unauthorized(
                        exchange,
                        validationResponse.getError() != null ? validationResponse.getError() : "Invalid token");
            }
        });
    }

    private String extractToken(HttpHeaders headers) {
        List<String> authHeaders = headers.get(securityProperties.getTokenHeader());
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        String authHeader = authHeaders.getFirst();
        if (authHeader != null && authHeader.startsWith(securityProperties.getTokenPrefix())) {
            return authHeader
                    .substring(securityProperties.getTokenPrefix().length())
                    .trim();
        }
        return null;
    }

    @Setter
    @Getter
    public static class Config {
        // Можно добавить конфигурационные параметры если нужно
        private boolean requireAuth = true;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String responseBody = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message);

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(responseBody.getBytes())));
    }
}
