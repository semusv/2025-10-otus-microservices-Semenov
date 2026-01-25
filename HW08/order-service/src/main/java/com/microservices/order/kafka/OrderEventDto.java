package com.microservices.order.kafka;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderEventDto {

    @NotNull(message = "orderId is required")
    private UUID orderId;

    @NotNull(message = "userId is required")
    private UUID userId;

    @Email(message = "email should be a valid email address")
    @NotBlank(message = "email is required")
    @Size(max = 255, message = "email must not exceed 255 characters")
    private String email;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.01", message = "price must be greater than or equal to 0.01")
    @DecimalMin(value = "0.00", inclusive = false, message = "price must be greater than 0")
    @Digits(
            integer = 10,
            fraction = 2,
            message = "price integer part must be at most 10 digits and fraction part at most 2 digits")
    private BigDecimal price;

    @NotNull(message = "status is required")
    private Status status;

    @Size(max = 1000, message = "message must not exceed 1000 characters")
    private String message; // Опционально - для дополнительной информации

    public enum Status {
        PAID,
        FAILED,
        PENDING
    }
}
