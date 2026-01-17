package com.microservices.auth.service;

import ru.vvsem.shared.dto.shared_api_dto.AuthLoginRequest;
import ru.vvsem.shared.dto.shared_api_dto.AuthRegisterRequest;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenResponse;

public interface AuthService {

    void register(AuthRegisterRequest request);

    AuthTokenResponse login(AuthLoginRequest request);

    void logout(String refreshTokenValue);
}
