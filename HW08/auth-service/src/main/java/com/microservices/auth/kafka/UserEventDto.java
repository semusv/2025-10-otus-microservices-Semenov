package com.microservices.auth.kafka;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserEventDto {

    @Builder.Default
    @NotBlank(message = "source is required")
    private String source = "auth-service";

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotBlank(message = "email is required")
    @Email(message = "email should be valid")
    private String email;

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
    private String username;

    public enum EventType {
        USER_CREATED
    }
}
