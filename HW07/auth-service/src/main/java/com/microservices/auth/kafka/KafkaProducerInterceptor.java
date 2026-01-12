package com.microservices.auth.kafka;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

@Slf4j
@NoArgsConstructor
public class KafkaProducerInterceptor implements ProducerInterceptor<String, Object> {

    private String serviceName;

    private String requestIdHeader;

    private String serviceNameHeader;

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        record.headers().add(serviceNameHeader, serviceName.getBytes());
        record.headers().add(requestIdHeader, MDC.get(requestIdHeader).getBytes());
        record.headers().add("X-Event-Id", UUID.randomUUID().toString().getBytes());
        record.headers()
                .add(
                        "X-Event-Timestamp",
                        OffsetDateTime.now(ZoneOffset.UTC).toString().getBytes());
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata recordMetadata, Exception e) {
        return;
    }

    @Override
    public void close() {
        return;
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // Получаем кастомные свойства из Kafka конфигурации
        this.serviceName = (String) configs.get("custom.service.name");
        this.requestIdHeader = (String) configs.get("custom.request.id.header");
        this.serviceNameHeader = (String) configs.get("custom.service.name.header");
    }
}
