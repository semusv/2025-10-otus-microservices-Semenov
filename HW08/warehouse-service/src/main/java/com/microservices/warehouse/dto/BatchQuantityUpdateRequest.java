package com.microservices.warehouse.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на массовое обновление количества")
public class BatchQuantityUpdateRequest {

    @NotEmpty(message = "Список обновлений не может быть пустым")
    @Schema(description = "Список обновлений")
    private List<@Valid BatchUpdateItem> updates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Элемент массового обновления")
    public static class BatchUpdateItem {

        @NotNull(message = "ID каталога обязательно")
        @Schema(description = "ID элемента каталога", example = "123e4567-e89b-12d3-a456-426614174000")
        private UUID catalogId;

        @NotNull(message = "Количество обязательно")
        @Min(value = 0, message = "Количество не может быть отрицательным")
        @Schema(description = "Количество для добавления", example = "10", minimum = "0")
        private Integer quantity;
    }
}
