package com.microservices.user.dto;

import com.microservices.user.model.UserProfile;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for {@link UserProfile}
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private UUID userId;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private LocalDate dateOfBirth;

    private UserProfile.ProfileStatus status;
}
