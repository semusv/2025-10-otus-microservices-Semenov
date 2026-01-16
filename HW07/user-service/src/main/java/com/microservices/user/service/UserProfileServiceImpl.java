package com.microservices.user.service;

import com.microservices.user.mapper.ProfileResponseMapper;
import com.microservices.user.model.UserProfile;
import com.microservices.user.repository.UserProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vvsem.shared.dto.shared_api_dto.UserCreateProfileRequest;
import ru.vvsem.shared.dto.shared_api_dto.UserProfileResponse;
import ru.vvsem.shared.dto.shared_api_dto.UserUpdateProfileRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;

    private final ProfileResponseMapper profileResponseMapper;

    /**
     * Создание профиля при регистрации (вызывается из auth-service)
     */
    @Transactional
    @Override
    public void createProfile(UserCreateProfileRequest request, UUID userId) {
        log.info("Creating profile for user: {}", request.getUserId());
        checksBeforeCreation(request, userId);
        UserProfile profile = new UserProfile();
        profile.setUserId(request.getUserId());
        profile.setEmail(request.getEmail());
        profile.setUsername(request.getUsername());
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhone(request.getPhone());
        profile.setStatus(UserProfile.ProfileStatus.ACTIVE);

        userProfileRepository.save(profile);
        log.info("Profile created successfully for user: {}", request.getUserId());
    }

    private void checksBeforeCreation(UserCreateProfileRequest request, UUID userId) {
        // Проверяем, что ID пользователя совпадает с ID из запроса
        if (!userId.equals(request.getUserId())) {
            throw new IllegalStateException(" User ID mismatch : " + userId + " != " + request.getUserId() + " ");
        }
        // Проверяем, что профиль еще не существует
        if (userProfileRepository.existsByUserId(request.getUserId())) {
            throw new IllegalStateException("Profile already exists for user: " + request.getUserId());
        }
        // Проверяем уникальность email и username
        if (userProfileRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already exists: " + request.getEmail());
        }
        if (userProfileRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username already exists: " + request.getUsername());
        }
    }

    /**
     * Получение профиля по ID пользователя
     */
    private UserProfile getProfileByUserId(UUID userId) {
        return userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));
    }

    /**
     * Получение профиля по UserName
     */
    private UserProfile getProfileByUsername(String username) {
        return userProfileRepository
                .findByUsername(username)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + username));
    }

    /**
     * Обновление профиля пользователя
     * Проверяет, что пользователь обновляет только свой профиль
     */
    @Transactional
    @Override
    public UserProfileResponse updateProfile(UUID userId, UserUpdateProfileRequest request) {
        UserProfile existingProfile = getProfileByUserId(userId);
        // Обновляем только разрешенные поля
        if (request.getFirstName() != null) {
            existingProfile.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            existingProfile.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            existingProfile.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            existingProfile.setEmail(request.getEmail());
        }
        if (request.getDateOfBirth() != null) {
            existingProfile.setDateOfBirth(request.getDateOfBirth());
        }

        return profileResponseMapper.toProfileResponse(userProfileRepository.save(existingProfile));
    }

    /**
     * Удаление профиля (soft delete через статус)
     */
    @Transactional
    @Override
    public void deactivateProfile(UUID userId) {
        UserProfile profile = getProfileByUserId(userId);
        profile.setStatus(UserProfile.ProfileStatus.INACTIVE);
        userProfileRepository.save(profile);
        log.info("Profile deactivated for user: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileResponseByUserId(UUID userId) {
        return profileResponseMapper.toProfileResponse(getProfileByUserId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileResponseByUsername(String username, UUID userId) {
        UserProfile profile = getProfileByUsername(username);
        if (!profile.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can see only your profile, not for user: " + username);
        }
        return profileResponseMapper.toProfileResponse(getProfileByUsername(username));
    }

    // Кастомное исключение
    public static class ProfileNotFoundException extends RuntimeException {
        public ProfileNotFoundException(String message) {
            super(message);
        }
    }
}
