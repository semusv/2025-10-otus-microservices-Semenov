package com.microservices.user.dto;

import com.microservices.user.model.UserProfile;
import java.time.LocalDate;
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
public class UpdateProfileRequest {
    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private LocalDate dateOfBirth;
}
