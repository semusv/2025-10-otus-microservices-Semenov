package com.microservices.auth.controller;

import com.microservices.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.vvsem.shared.dto.shared_api_dto.AuthLoginRequest;
import ru.vvsem.shared.dto.shared_api_dto.AuthRefreshTokenRequest;
import ru.vvsem.shared.dto.shared_api_dto.AuthRegisterRequest;
import ru.vvsem.shared.dto.shared_api_dto.AuthTokenResponse;
import ru.vvsem.shared.dto.shared_api_dto.AuthValidateTokenRequest;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody AuthRegisterRequest request) {
        authService.register(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthTokenResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public AuthTokenResponse refreshToken(@Valid @RequestBody AuthRefreshTokenRequest request) {
        return authService.refreshToken(request.getRefreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody AuthRefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
    }

    @PostMapping("/validate")
    @ResponseStatus(HttpStatus.OK)
    public Boolean validateToken(@Valid @RequestBody AuthValidateTokenRequest request) {
        // Этот endpoint может использоваться Gateway для проверки токенов
        return authService.validateToken(request.getToken());
    }
}
