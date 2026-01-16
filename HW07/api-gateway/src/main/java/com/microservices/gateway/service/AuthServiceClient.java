package com.microservices.gateway.service;

import reactor.core.publisher.Mono;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenValidationResponse;

public interface AuthServiceClient {

    Mono<AuthTokenValidationResponse> validateToken(String token);

    boolean checkValidAccessToken(String tokenType, boolean valid);
}
