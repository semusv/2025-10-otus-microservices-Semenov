package com.microservices.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.Data;

@Data
public class TokenValidationResponse {
    private boolean valid;

    @JsonProperty("userId")
    private String userId;

    private String username;

    private Set<String> roles;

    private String tokenType;

    private String error;

    public boolean isValidAccessToken() {
        return valid && "access".equals(tokenType);
    }
}
