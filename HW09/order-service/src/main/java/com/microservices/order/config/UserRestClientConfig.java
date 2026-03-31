package com.microservices.order.config;

import com.microservices.order.config.properties.SecurityProperties;
import com.microservices.order.config.properties.ServicesProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
@Order(2)
public class UserRestClientConfig {

    private final SecurityProperties securityProperties;

    private final ServicesProperties servicesProperties;

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${rest.client.connect-timeout:5s}")
    private Duration connectTimeout;

    @Value("${rest.client.read-timeout:10s}")
    private Duration readTimeout;

    @Bean
    public RestClient userServiceRestClient() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(servicesProperties.getServices().get("user-service").getUrl())
                .requestFactory(requestFactory)
                .defaultHeader(securityProperties.getServiceNameHeader(), serviceName)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(securityProperties.getServiceSecretHeader(), securityProperties.getSecret())
                .requestInterceptor((request, body, execution) -> {
                    // Логирование запросов
                    System.out.println("Sending request to: " + request.getURI());
                    System.out.println("Method: " + request.getMethod());
                    return execution.execute(request, body);
                })
                .defaultStatusHandler(status -> status.value() == 404, (request, response) -> {
                    throw new RuntimeException("User service endpoint not found");
                })
                .defaultStatusHandler(status -> status.value() == 503, (request, response) -> {
                    throw new RuntimeException("User service unavailable");
                })
                .build();
    }
}
