package com.microservices.auth.controller;

import com.microservices.auth.dto.auth.TokenValidationResponse;
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
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final AuthService authService;

    /**
     * ВНУТРЕННИЙ ЭНДПОИНТ
     * Валидация и возврат информации о пользователе по токену
     */
    @PostMapping("/validate")
    @ResponseStatus(HttpStatus.OK)
    public TokenValidationResponse validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        // Этот endpoint может использоваться Gateway для проверки токенов
        return authService.validateTokenWithData(request.getToken());
    }
}
