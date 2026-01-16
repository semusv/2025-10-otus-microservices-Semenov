package com.microservices.user.service;

import java.util.UUID;

import ru.vvsem.shared.dto.shared_api_dto.UserCreateProfileRequest;
import ru.vvsem.shared.dto.shared_api_dto.UserProfileResponse;
import ru.vvsem.shared.dto.shared_api_dto.UserUpdateProfileRequest;

public interface UserProfileService {

    void createProfile(UserCreateProfileRequest request, UUID userId);

    UserProfileResponse updateProfile(UUID userId, UserUpdateProfileRequest request);

    void deactivateProfile(UUID userId);

    UserProfileResponse getProfileResponseByUserId(UUID userId);

    UserProfileResponse getProfileResponseByUsername(String username, UUID userId);
}
