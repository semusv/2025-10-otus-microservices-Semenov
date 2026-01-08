package com.microservices.auth.dto.auth;

import java.util.Set;
import lombok.Data;

@Data
public class TokenValidationResponse {
    private boolean valid;

    private String userId;

    private String username;

    private Set<String> roles;

    private String tokenType;

    private String error;
}
