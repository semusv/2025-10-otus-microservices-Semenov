package com.microservices.auth.service;

import com.microservices.auth.client.UserServiceClient;
import com.microservices.auth.comonents.security.JwtTokenProvider;
import com.microservices.auth.config.properties.SecurityProperties;
import com.microservices.auth.event.UserCreatedEvent;
import com.microservices.auth.model.RefreshToken;
import com.microservices.auth.model.Role;
import com.microservices.auth.model.User;
import com.microservices.auth.repository.RefreshTokenRepository;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vvsem.shared.dto.shared_api_dto.AuthLoginRequest;
import ru.vvsem.shared.dto.shared_api_dto.AuthRegisterRequest;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenResponse;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenValidationResponse;
import ru.vvsem.shared.dto.shared_api_dto.UserCreateProfileRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Value("${spring.application.name}")
    private String serviceName;

    private final SecurityProperties securityProperties;

    private final UserRepository userRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final AuthenticationManager authenticationManager;

    private final CustomUserDetailsService userDetailsService;

    private final UserServiceClient userServiceClient;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public void register(AuthRegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        // 1. Создаем пользователя в auth DB
        User savedUser = createUser(request);
        // 2. Создаем профиль в user-service и публикуем событие
        try {
            callCreationUserProfile(request, savedUser);
            eventPublisher.publishEvent(
                    new UserCreatedEvent(savedUser, serviceName, MDC.get(securityProperties.getRequestIdHeader())));
        } catch (Exception e) {
            // Если не удалось создать профиль - откатываем регистрацию
            userRepository.delete(savedUser);
            log.error("Failed to create user profile, rolling back user registration", e);
            throw new RuntimeException("Failed to create user profile: " + e.getMessage());
        }
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

        return generateTokens(user);
    }

    @Transactional
    @Override
    public AuthTokenResponse refreshToken(String refreshTokenValue) {
        // Проверяем refresh token в БД
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        // Помечаем старый токен как использованный
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Генерируем новые токены
        User user = refreshToken.getUser();
        return generateTokens(user);
    }

    @Transactional
    @Override
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public Boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    @Override
    public AuthTokenValidationResponse validateTokenWithData(String token) {
        // Делаем валидацию токена и возвращаем данные пользователя
        if (Boolean.TRUE.equals(validateToken(token))) {
            AuthTokenValidationResponse response = new AuthTokenValidationResponse();
            response.setUsername(jwtTokenProvider.getUsernameFromToken(token));
            response.setRoles(jwtTokenProvider.getRolesFromToken(token));
            response.setValid(true);
            response.setUserId(jwtTokenProvider.getUserIdFromToken(token));
            response.setTokenType("access");

            return response;
        } else {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    private AuthTokenResponse generateTokens(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String accessToken = jwtTokenProvider.generateAccessToken(userDetails, user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails, user.getId());
        // Отзываем старые токены
        refreshTokenRepository.findByUserIdAndRevoked(user.getId(), false).forEach(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
        // Сохраняем refresh token в БД
        saveRefreshToken(user, refreshToken);

        return new AuthTokenResponse()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(
                        jwtTokenProvider.getExpirationDateFromToken(accessToken).getTime())
                .tokenType("Bearer");
    }

    private void saveRefreshToken(User user, String tokenValue) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(tokenValue);
        refreshToken.setExpiryDate(
                jwtTokenProvider.getExpirationDateFromToken(tokenValue).toInstant());

        refreshTokenRepository.save(refreshToken);
    }
}
