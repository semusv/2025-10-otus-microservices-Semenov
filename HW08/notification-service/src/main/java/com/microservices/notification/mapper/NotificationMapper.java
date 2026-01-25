package com.microservices.notification.mapper;

import com.microservices.notification.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.vvsem.shared.dto.shared_api_dto.NotificationResponse;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {
    Notification toEntity(NotificationResponse notificationResponse);

    NotificationResponse toNotificationResponse(Notification notification);
}
