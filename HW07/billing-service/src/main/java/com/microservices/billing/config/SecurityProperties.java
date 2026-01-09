package com.microservices.billing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.security.inter-service")
@Getter
@Setter
public class SecurityProperties {

    private String secret;

    private String userIdHeader = "X-User-Id";

    private String rolesHeader = "X-Roles";

    private String serviceSecretHeader = "X-Service-Secret";

    private String serviceNameHeader = "X-Service-Name";

    private String requestIdHeader = "X-Request-Id";
}
