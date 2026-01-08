package com.microservices.auth.dto.auth;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TokenResponse {
    private final String accessToken;

    private final String refreshToken;

    private String tokenType = "Bearer";

    private final Long expiresIn;
}
