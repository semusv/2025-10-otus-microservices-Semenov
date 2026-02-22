package com.microservices.order.config;

import com.microservices.order.config.properties.SecurityProperties;
import com.microservices.order.filter.MdcFilter;
import com.microservices.order.filter.ServiceAuthFilter;
import com.microservices.order.filter.UserAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@SuppressWarnings("CheckStyle")
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityProperties securityProperties;

    public SecurityConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    public ServiceAuthFilter serviceAuthFilter() {
        return new ServiceAuthFilter(securityProperties);
    }

    @Bean
    public UserAuthFilter userAuthFilter() {
        return new UserAuthFilter(securityProperties);
    }

    @Bean
    public MdcFilter mdcFilter() {
        return new MdcFilter(securityProperties);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return authentication -> {
            throw new AuthenticationServiceException(
                    "Authentication is handled by filters. This should not be called.");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        configureCsrfAndSession(http);
        configureAuthorization(http);
        configureFilters(http);
        return http.build();
    }

    private void configureCsrfAndSession(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }

    private void configureAuthorization(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                // Внутрисервисные эндпоинты - только для сервисов
                .requestMatchers("/internal/**")
                .hasRole("SERVICE")

                // Защищенные эндпоинты - требуют аутентификации через X-USER
                .requestMatchers("/orders/**")
                .authenticated()

                // Публичные эндпоинты
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml/**",
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/springwolf/**",
                        "/actuator/**")
                .permitAll()
                .anyRequest()
                .denyAll());
    }

    private void configureFilters(HttpSecurity http) {
        http.addFilterBefore(serviceAuthFilter(), AuthorizationFilter.class)
                .addFilterBefore(userAuthFilter(), ServiceAuthFilter.class)
                .addFilterBefore(mdcFilter(), UserAuthFilter.class);
    }
}
