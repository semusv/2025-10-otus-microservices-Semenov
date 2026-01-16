package com.microservices.auth.service;

import ru.vvsem.shared.dto.shared_api_dto.AuthLoginRequest;
import ru.vvsem.shared.dto.shared_api_dto.AuthRegisterRequest;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenResponse;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenValidationResponse;

public interface AuthService {

    void register(AuthRegisterRequest request);

    AuthTokenResponse login(AuthLoginRequest request);

    AuthTokenResponse refreshToken(String refreshTokenValue);

    void logout(String refreshTokenValue);

    Boolean validateToken(String token);

    AuthTokenValidationResponse validateTokenWithData(String token);
}
