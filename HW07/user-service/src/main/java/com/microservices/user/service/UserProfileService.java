package com.microservices.user.service;

import com.microservices.user.dto.CreateProfileRequest;
import com.microservices.user.dto.ProfileResponse;
import com.microservices.user.dto.UpdateProfileRequest;
import java.util.UUID;

public interface UserProfileService {

    void createProfile(CreateProfileRequest request, UUID userId);

    ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void deactivateProfile(UUID userId);

    ProfileResponse getProfileResponseByUserId(UUID userId);

    ProfileResponse getProfileResponseByUsername(String username, UUID userId);
}
