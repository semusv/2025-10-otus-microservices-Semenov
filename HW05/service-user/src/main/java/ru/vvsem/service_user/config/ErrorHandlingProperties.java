package ru.vvsem.service_user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "error-handling")
public class ErrorHandlingProperties {

    private boolean enabled = false;
    private double probability = 0.01; // 1% по умолчанию

    public void setProbability(double probability) {
        if (probability < 0 || probability > 1) {
            throw new IllegalArgumentException("Probability must be between 0 and 1");
        }
        this.probability = probability;
    }
}
