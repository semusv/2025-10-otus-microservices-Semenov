package ru.vvsem.service_user.dto;

import java.time.LocalDate;
import lombok.Value;
import ru.vvsem.service_user.model.User;

/**
 * DTO for {@link User}
 */
@Value
public class UserNewDto {
    String firstName;
    String lastName;
    LocalDate birthDate;
    String email;
    String phoneNumber;
}
