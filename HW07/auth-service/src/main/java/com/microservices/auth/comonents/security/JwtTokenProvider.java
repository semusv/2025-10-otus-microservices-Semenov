package com.microservices.auth.comonents.security;

import com.microservices.auth.model.User;
import java.util.Date;
import java.util.Set;

public interface JwtTokenProvider {
    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    Boolean validateToken(String token);

    String getUsernameFromToken(String token);

    String getUserIdFromToken(String token);

    Set<String> getRolesFromToken(String token);

    Date getExpirationDateFromToken(String token);

    boolean isTokenExpired(String token);
}
