package com.microservices.user.controller;

import com.microservices.user.dto.UpdateProfileRequest;
import com.microservices.user.dto.UserProfileResponse;
import com.microservices.user.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    /**
     * Получение своего профиля
     * Gateway передает X-User-Id в заголовке
     */
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileResponse getMyProfile(@RequestHeader("X-User-Id") UUID userId) {
        return userProfileService.getProfileResponseByUserId(userId);
    }

    /**
     * Получение профиля по ID
     * Можно получать только свой профиль (проверка в сервисе)
     */
    @GetMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal")
    public ResponseEntity<UserProfileResponse> getProfile(
            @PathVariable UUID userId, @RequestHeader("X-User-Id") UUID authenticatedUserId) {

        // Двойная проверка: Spring Security + ручная
        if (!userId.equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var profile = userProfileService.getProfileResponseByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Обновление своего профиля
     */
    @PutMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileResponse updateMyProfile(
            @RequestHeader("X-User-Id") UUID userId, @Valid @RequestBody UpdateProfileRequest request) {

        return userProfileService.updateProfile(userId, request);
    }

    /**
     * Деактивация профиля
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public void deactivateMyProfile(@RequestHeader("X-User-Id") UUID userId) {
        userProfileService.deactivateProfile(userId);
    }

    /**
     * Получение профиля по username (публичный)
     */
    @GetMapping("/username/{username}")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileResponse getProfileByUsername(
            @PathVariable String username, @RequestHeader("X-User-Id") UUID userId) {
        return userProfileService.getProfileResponseByUsername(username, userId);
    }
}
