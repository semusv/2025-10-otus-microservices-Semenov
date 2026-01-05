package com.microservices.auth.service;

import com.microservices.auth.dto.auth.LoginRequest;
import com.microservices.auth.dto.auth.RegisterRequest;
import com.microservices.auth.dto.auth.TokenResponse;
import com.microservices.auth.dto.auth.TokenValidationResponse;

public interface AuthService {

    void register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refreshToken(String refreshTokenValue);

    void logout(String refreshTokenValue);

    Boolean validateToken(String token);

    TokenValidationResponse validateTokenWithData(String token);
}
