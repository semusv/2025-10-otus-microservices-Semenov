package com.microservices.auth.service;

import com.microservices.auth.client.UserServiceClient;
import com.microservices.auth.event.UserCreatedEvent;
import com.microservices.auth.model.Role;
import com.microservices.auth.model.User;
import com.microservices.auth.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vvsem.shared.dto.shared_api_dto.AuthLoginRequest;
import ru.vvsem.shared.dto.shared_api_dto.AuthRegisterRequest;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenResponse;
import ru.vvsem.shared.dto.shared_api_dto.UserCreateProfileRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Value("${spring.application.name}")
    private String serviceName;

    private final TokenService tokenService;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final UserServiceClient userServiceClient;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public void register(AuthRegisterRequest request) {
        // 1. Создаем пользователя в auth DB
        User savedUser = validateAndCreateUser(request);
        // 2. Создаем профиль в user-service и публикуем событие
        try {
            callCreationUserProfile(request, savedUser);
            eventPublisher.publishEvent(new UserCreatedEvent(savedUser, serviceName, MDC.getCopyOfContextMap()));
        } catch (Exception e) {
            // Если не удалось создать профиль - откатываем регистрацию
            userRepository.delete(savedUser);
            log.error("Failed to create user profile, rolling back user registration", e);
            throw new RuntimeException("Failed to create user profile: " + e.getMessage());
        }
    }

    private User validateAndCreateUser(AuthRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        return createUser(request);
    }

    @Override
    @Transactional
    public AuthTokenResponse login(AuthLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository
                .findByUsername(request.getLogin())
                .or(() -> userRepository.findByEmail(request.getLogin()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return tokenService.generateTokens(user);
    }

    @Transactional
    @Override
    public void logout(String refreshTokenValue) {
        tokenService.revokeToken(refreshTokenValue);
    }

    private User createUser(AuthRegisterRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(Role.USER))
                .build();
        User savedUser = userRepository.save(user);
        log.info("User created in auth DB: {}", savedUser.getId());
        return savedUser;
    }

    private void callCreationUserProfile(AuthRegisterRequest request, User savedUser) {

        var profileCreationRequest = new UserCreateProfileRequest()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone());

        // Вызов user-service для создания профиля через HTTP клиент
        userServiceClient.createUserProfile(profileCreationRequest);
        log.info("User profile created for user id: {}", savedUser.getId());
    }
}
