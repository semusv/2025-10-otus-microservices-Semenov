package com.microservices.auth.dto.auth;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileCreationRequest {
    private UUID userId;

    private String email;

    private String username;

    private String firstName;

    private String lastName;

    private String phone;
}
