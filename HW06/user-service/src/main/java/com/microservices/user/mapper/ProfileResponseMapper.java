package com.microservices.user.mapper;

import com.microservices.user.dto.ProfileResponse;
import com.microservices.user.model.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProfileResponseMapper {
    UserProfile toEntity(ProfileResponse profileResponse);

    ProfileResponse toProfileResponse(UserProfile userProfile);
}
