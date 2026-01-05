package com.microservices.auth.service;

import com.microservices.auth.client.UserServiceClient;
import com.microservices.auth.comonents.security.JwtTokenProvider;
import com.microservices.auth.dto.auth.LoginRequest;
import com.microservices.auth.dto.auth.ProfileCreationRequest;
import com.microservices.auth.dto.auth.RegisterRequest;
import com.microservices.auth.dto.auth.TokenResponse;
import com.microservices.auth.dto.auth.TokenValidationResponse;
import com.microservices.auth.model.RefreshToken;
import com.microservices.auth.model.Role;
import com.microservices.auth.model.User;
import com.microservices.auth.repository.RefreshTokenRepository;
import com.microservices.auth.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final AuthenticationManager authenticationManager;

    private final CustomUserDetailsService userDetailsService;

    private final UserServiceClient userServiceClient;

    @Transactional
    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        // 1. Создаем пользователя в auth DB
        User savedUser = createUser(request);

        // 2. Создаем профиль в user-service
        try {
            callCreationUserProfile(request, savedUser);
        } catch (Exception e) {
            // Если не удалось создать профиль - откатываем регистрацию
            userRepository.delete(savedUser);
            log.error("Failed to create user profile, rolling back user registration", e);
            throw new RuntimeException("Failed to create user profile: " + e.getMessage());
        }
    }

    private User createUser(RegisterRequest request) {
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

    private void callCreationUserProfile(RegisterRequest request, User savedUser) {
        // Вызов user-service для создания профиля через HTTP клиент
        userServiceClient.createUserProfile(new ProfileCreationRequest(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getUsername(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhone()));
        log.info("User profile created for user id: {}", savedUser.getId());
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
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
    public TokenResponse refreshToken(String refreshTokenValue) {
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
    public TokenValidationResponse validateTokenWithData(String token) {
        // Делаем валидацию токена и возвращаем данные пользователя
        if (Boolean.TRUE.equals(validateToken(token))) {
            TokenValidationResponse response = new TokenValidationResponse();
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

    private TokenResponse generateTokens(User user) {
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

        return new TokenResponse(
                accessToken,
                refreshToken,
                jwtTokenProvider.getExpirationDateFromToken(accessToken).getTime());
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
