package com.microservices.auth.dto.auth;

import com.microservices.auth.model.User;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

/**
 * DTO for {@link User}
 */
@Value
public class LoginRequest {
    @NotBlank(message = "Username or email is required")
    private String login;

    @NotBlank(message = "Password is required")
    private String password;
}
