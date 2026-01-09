package com.microservices.user.controller;

import com.microservices.user.dto.CreateProfileRequest;
import com.microservices.user.dto.ProfileResponse;
import com.microservices.user.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final UserProfileService userProfileService;

    /**
     * ВНУТРЕННИЙ ЭНДПОИНТ
     * Создание профиля при регистрации (вызывается из auth-service)
     * Требует сервисный секрет в заголовке X-Service-Secret
     */
    @PostMapping("/profile")
    @PreAuthorize("hasRole('SERVICE')")
    @ResponseStatus(HttpStatus.CREATED)
    public void createProfileInternal(
            @RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody CreateProfileRequest request) {
        userProfileService.createProfile(request, userId);
    }

    /**
     * Получение своего профиля
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('SERVICE')")
    @ResponseStatus(HttpStatus.OK)
    public ProfileResponse getMyProfile(@RequestHeader("X-User-Id") UUID userId) {
        return userProfileService.getProfileResponseByUserId(userId);
    }
}
