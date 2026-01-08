package com.microservices.gateway.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.inter-service")
@Getter
@Setter
public class SecurityProperties {

    private String secret;

    private String userIdHeader = "X-User-Id";

    private String rolesHeader = "X-Roles";

    private String serviceSecretHeader = "X-Service-Secret";

    private String serviceNameHeader = "X-Service-Name";

    private String tokenHeader = "Authorization";

    private String tokenPrefix = "Bearer ";

    private String requestIdHeader = "X-Request-Id";
}
