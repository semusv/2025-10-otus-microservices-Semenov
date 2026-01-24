package com.microservices.auth.comonents.security;

import java.util.Date;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtTokenProvider {
    String generateAccessToken(UserDetails userDetails, UUID userId);

    String generateRefreshToken(UserDetails userDetails, UUID userId);

    Boolean validateToken(String token);

    String getUsernameFromToken(String token);

    String getUserIdFromToken(String token);

    Set<String> getRolesFromToken(String token);

    Date getExpirationDateFromToken(String token);

    boolean isTokenExpired(String token);
}
