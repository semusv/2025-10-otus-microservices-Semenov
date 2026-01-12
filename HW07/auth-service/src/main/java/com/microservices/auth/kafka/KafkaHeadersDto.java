package com.microservices.auth.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "KafkaHeadersDto", description = "Common Kafka headers for events")
public class KafkaHeadersDto {

    @Schema(description = "Type of event", example = "USER_CREATED")
    @JsonProperty("X-EventType")
    private String eventType;
}
