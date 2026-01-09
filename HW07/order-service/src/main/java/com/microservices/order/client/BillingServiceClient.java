package com.microservices.order.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.order.config.properties.SecurityProperties;
import com.microservices.order.dto.BalanceResponse;
import com.microservices.order.dto.BillingOrderRequest;
import com.microservices.order.resolver.EndpointResolver;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingServiceClient {

    private final RestClient billingServiceRestClient;

    private final SecurityProperties securityProperties;

    private final EndpointResolver endpointResolver;

    @Value("${spring.application.name}")
    private String serviceName;

    @SuppressWarnings("CheckStyle")
    public void withdrawMoney(BillingOrderRequest billingOrderRequest) {
        log.info("Calling billing-service to withdraw money for user: {}", billingOrderRequest.getUserId());

        try {
            BalanceResponse balanceResponse = billingServiceRestClient
                    .post()
                    .uri(endpointResolver.getBillingServiceUrl("order"))
                    .header(
                            securityProperties.getUserIdHeader(),
                            billingOrderRequest.getUserId().toString())
                    .header(securityProperties.getRequestIdHeader(), MDC.get(securityProperties.getRequestIdHeader()))
                    .header(securityProperties.getServiceNameHeader(), serviceName)
                    .header(securityProperties.getServiceSecretHeader(), securityProperties.getSecret())
                    .body(billingOrderRequest)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.BAD_REQUEST, this::mapBillingError)
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, this::mapBillingError)
                    .body(BalanceResponse.class);

            assert balanceResponse != null;
            log.info(
                    "Successfully withdrawn money for: {}, balance: {}",
                    billingOrderRequest.getUserId(),
                    balanceResponse.getBalance().toString());
        } catch (Exception e) {
            log.error("Failed to withdrawn money for {}: {}", billingOrderRequest.getUserId(), e.getMessage());
            throw new BillingServiceException("Failed to call billing-service: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("CheckStyle")
    private void mapBillingError(HttpRequest request, ClientHttpResponse response) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> errorResponse = mapper.readValue(response.getBody(), new TypeReference<>() {});

            String errorCode = errorResponse.get("error");
            String message = errorResponse.get("message");

            log.error("Billing service error: {} - {}", errorCode, message);

            if ("INSUFFICIENT_FUNDS".equals(errorCode)) {
                throw new BillingServiceException(message != null ? message : "Insufficient funds for user");
            } else if ("RESOURCE_NOT_FOUND".equals(errorCode)) {
                throw new BillingServiceException(message != null ? message : "Account not found for user");
            } else {
                throw new BillingServiceException(
                        "Billing service error: " + (message != null ? message : response.getBody()));
            }

        } catch (Exception e) {
            throw new BillingServiceException("Failed to call billing-service: " + e.getMessage(), e);
        }
    }

    private static class BillingServiceException extends RuntimeException {
        public BillingServiceException(String message) {
            super(message);
        }

        public BillingServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
