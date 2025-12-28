package com.microservices.user.dto;

import com.microservices.user.model.UserProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for {@link UserProfile}
 */
@Getter
@Setter
public class CreateProfileRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Username is required")
    private String username;

    private String firstName;

    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    private String phone;

    private LocalDate dateOfBirth;
}
