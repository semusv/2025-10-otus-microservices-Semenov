package com.microservices.auth.service;

import com.microservices.auth.model.User;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenResponse;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenValidationResponse;

public interface TokenService {
    AuthTokenResponse refreshToken(String refreshTokenValue);

    Boolean validateToken(String token);

    AuthTokenValidationResponse validateTokenWithData(String token);

    AuthTokenResponse generateTokens(User user);

    void revokeToken(String token);
}
