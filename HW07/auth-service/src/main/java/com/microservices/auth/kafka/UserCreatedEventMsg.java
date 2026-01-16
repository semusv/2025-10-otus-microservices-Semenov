package com.microservices.auth.kafka;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserCreatedEventMsg {

    @Builder.Default
    private String source = "auth-service";

    private UUID userId;

    private String email;

    private String username;
}
