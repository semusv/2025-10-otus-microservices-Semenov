package ru.vvsem.service_user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import ru.vvsem.service_user.dto.UserFullDto;
import ru.vvsem.service_user.model.User;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface UserFullMapper {
    User toEntity(UserFullDto userFullDto);

    UserFullDto toUserFullDto(User user);

    void updateWithNull(UserFullDto userFullDto, @MappingTarget User user);
}
