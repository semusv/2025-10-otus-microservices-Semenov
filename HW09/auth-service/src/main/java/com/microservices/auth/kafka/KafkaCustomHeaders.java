package com.microservices.auth.kafka;

public class KafkaCustomHeaders {

    public static final String SOURCE = "X-Source";

    public static final String EVENT_ID = "X-Event-Id";

    public static final String EVENT_TYPE = "X-Event-Type";

    public static final String TIMESTAMP = "X-Timestamp";

    public static final String REQUEST_ID = "X-Request-Id";

    // Типы событий
    public enum EventType {
        ORDER_CREATED
    }
}
