package com.microservices.auth.config;

import com.microservices.auth.config.properties.SecurityProperties;
import com.microservices.auth.filter.MdcFilter;
import com.microservices.auth.filter.ServiceAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

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
    public MdcFilter mdcFilter() {
        return new MdcFilter(securityProperties);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        configureCsrfAndSession(http);
        configureAuthorization(http);
        configureFilters(http);
        return http.build();
    }

    private void configureFilters(HttpSecurity http) {
        http.addFilterBefore(serviceAuthFilter(), AuthorizationFilter.class)
                .addFilterBefore(mdcFilter(), ServiceAuthFilter.class);
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

                // Публичные служебные эндпоинты
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
                // Публичные эндпоинты аутентификации
                .requestMatchers("/auth/register", "/auth/login", "/auth/refresh", "/auth/validate", "/auth/logout")
                .permitAll()
                .anyRequest()
                .authenticated());
    }
}
