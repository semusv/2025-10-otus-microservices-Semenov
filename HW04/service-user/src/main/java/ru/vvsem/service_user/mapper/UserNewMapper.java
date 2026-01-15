package ru.vvsem.service_user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.vvsem.service_user.dto.UserNewDto;
import ru.vvsem.service_user.model.User;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserNewMapper {
    User toEntity(UserNewDto userNewDto);

    UserNewDto toUserNewDto(User user);
}
