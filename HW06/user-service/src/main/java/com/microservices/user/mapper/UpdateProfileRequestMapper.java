package com.microservices.user.mapper;

import com.microservices.user.dto.UpdateProfileRequest;
import com.microservices.user.model.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UpdateProfileRequestMapper {
    UserProfile toEntity(UpdateProfileRequest updateProfileRequest);

    UpdateProfileRequest toUpdateProfileRequest(UserProfile userProfile);
}
