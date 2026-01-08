package com.microservices.auth.controller;

import com.microservices.auth.dto.auth.LoginRequest;
import com.microservices.auth.dto.auth.RefreshTokenRequest;
import com.microservices.auth.dto.auth.RegisterRequest;
import com.microservices.auth.dto.auth.TokenResponse;
import com.microservices.auth.dto.auth.ValidateTokenRequest;
import com.microservices.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.getRefreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public Void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return null;
    }

    @PostMapping("/validate")
    @ResponseStatus(HttpStatus.OK)
    public Boolean validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        // Этот endpoint может использоваться Gateway для проверки токенов
        return authService.validateToken(request.getToken());
    }
}
