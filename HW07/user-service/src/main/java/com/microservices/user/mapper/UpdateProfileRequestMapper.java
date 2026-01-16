package com.microservices.user.mapper;

import com.microservices.user.model.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.vvsem.shared.dto.shared_api_dto.UserUpdateProfileRequest;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UpdateProfileRequestMapper {
    UserProfile toEntity(UserUpdateProfileRequest updateProfileRequest);

    UserUpdateProfileRequest toUpdateProfileRequest(UserProfile userProfile);
}
