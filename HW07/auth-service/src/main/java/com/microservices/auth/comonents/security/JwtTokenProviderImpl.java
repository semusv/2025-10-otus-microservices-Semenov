package com.microservices.auth.comonents.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenProviderImpl implements JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(UserDetails userDetails, UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put(
                "roles",
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));

        return buildToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    @Override
    public String generateRefreshToken(UserDetails userDetails, UUID userId) {

        String uniquePart = UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("tokenType", "refresh");
        claims.put("jti", uniquePart);

        return buildToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }

    @Override
    public Boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public String getUserIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    @Override
    public Set<String> getRolesFromToken(String token) {
        Claims claims = extractAllClaims(token);

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        return new HashSet<>(roles);
    }

    @Override
    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public boolean isTokenExpired(String token) {
        return getExpirationDateFromToken(token).before(new Date());
    }

    private String buildToken(Map<String, Object> extraClaims, String username, long expiration) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expiration);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
