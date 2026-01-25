package com.microservices.warehouse.model;

public enum ReservationStatus {
    PENDING, // Резервирование создано
    RESERVED, // Товары зарезервированы
    CONFIRMED, // Резервирование подтверждено (транзакция завершена)
    CANCELLED, // Резервирование отменено
    EXPIRED // Резервирование истекло
}
