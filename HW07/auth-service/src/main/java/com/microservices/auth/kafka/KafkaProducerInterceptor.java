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
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> recordKafka) {
        recordKafka.headers().add(serviceNameHeader, serviceName.getBytes());
        recordKafka.headers().add(requestIdHeader, MDC.get(requestIdHeader).getBytes());
        recordKafka.headers().add("X-Event-Id", UUID.randomUUID().toString().getBytes());
        recordKafka
                .headers()
                .add(
                        "X-Event-Timestamp",
                        OffsetDateTime.now(ZoneOffset.UTC).toString().getBytes());
        return recordKafka;
    }

    @Override
    public void onAcknowledgement(RecordMetadata recordMetadata, Exception e) {
        // Пока не требуется
    }

    @Override
    public void close() {
        // Пока не требуется
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // Получаем кастомные свойства из Kafka конфигурации
        this.serviceName = (String) configs.get("custom.service.name");
        this.requestIdHeader = (String) configs.get("custom.request.id.header");
        this.serviceNameHeader = (String) configs.get("custom.service.name.header");
    }
}
