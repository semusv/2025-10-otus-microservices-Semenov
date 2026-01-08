package com.microservices.gateway.service;

import com.microservices.gateway.dto.TokenValidationResponse;
import reactor.core.publisher.Mono;

public interface AuthServiceClient {

    Mono<TokenValidationResponse> validateToken(String token);
}
