package com.microservices.auth.service;

import com.microservices.auth.comonents.security.JwtTokenProvider;
import com.microservices.auth.model.RefreshToken;
import com.microservices.auth.model.User;
import com.microservices.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenResponse;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenValidationResponse;

@RequiredArgsConstructor
@Service
public class TokenServiceImpl implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    @Override
    public AuthTokenResponse refreshToken(String refreshTokenValue) {
        // Проверяем refresh token в БД
        RefreshToken oldRefreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (!oldRefreshToken.isValid()) {
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }
        // Генерируем новые токены
        User user = oldRefreshToken.getUser();
        return prepareTokenResponseWithNewTokens(user);
    }

    private AuthTokenResponse prepareTokenResponseWithNewTokens(User user) {

        String accessToken = generateNewAccessToken(user);
        String refreshToken = generateNewRefreshToken(user);
        return new AuthTokenResponse()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(
                        jwtTokenProvider.getExpirationDateFromToken(accessToken).getTime())
                .tokenType("Bearer");
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokenValidationResponse validateTokenWithData(String token) {
        // Делаем валидацию токена и возвращаем данные пользователя
        if (Boolean.TRUE.equals(jwtTokenProvider.validateToken(token))) {
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

    @Override
    @Transactional
    public AuthTokenResponse generateTokens(User user) {
        return prepareTokenResponseWithNewTokens(user);
    }

    @Override
    @Transactional
    public void revokeToken(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private String generateNewAccessToken(User user) {
        return jwtTokenProvider.generateAccessToken(user);
    }

    private String generateNewRefreshToken(User user) {
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        // Отзываем старые токены
        refreshTokenRepository.findByUserIdAndRevoked(user.getId(), false).forEach(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
        // Сохраняем refresh token в БД
        saveRefreshToken(user, refreshToken);
        return refreshToken;
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
