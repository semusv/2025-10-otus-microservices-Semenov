package com.microservices.billing.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.MDC;

@Slf4j
public class KafkaConsumerInterceptor implements ConsumerInterceptor<String, Object> {

    private String serviceName;

    private String requestIdHeader;

    private String serviceNameHeader;

    @Override
    public ConsumerRecords<String, Object> onConsume(ConsumerRecords<String, Object> events) {
        for (var event : events) {
            try {
                // 1. Устанавливаем контекст из headers
                setupContextFromHeaders(event);
                // 2. Валидируем сообщение
                validateMessage(event);
                // 3. Логируем получение сообщения
                logIncomingMessage(event);
                // 4. Добавляем consumed timestamp в headers
                event.headers()
                        .add(
                                "consumedTimestamp",
                                String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
                // 5. Добавляем consumer service name
                event.headers().add("consumerServiceName", serviceName.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.error("Error in consumer interceptor for topic {}: {}", event.topic(), e.getMessage(), e);
            }
        }
        return events;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        offsets.forEach((tp, offset) -> {
            log.debug("Committed offset {} for topic {} partition {}", offset.offset(), tp.topic(), tp.partition());
        });
    }

    @Override
    public void close() {
        MDC.clear();
        log.debug("KafkaConsumerInterceptor closed");
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // Получаем кастомные свойства из Kafka конфигурации
        this.serviceName = (String) configs.get("custom.service.name");
        this.requestIdHeader = (String) configs.get("custom.request.id.header");
        this.serviceNameHeader = (String) configs.get("custom.service.name.header");
    }

    private void setupContextFromHeaders(ConsumerRecord<String, Object> record) {
        MDC.clear();
        record.headers().headers(requestIdHeader).forEach(header -> {
            MDC.put(requestIdHeader, new String(header.value(), StandardCharsets.UTF_8));
        });
    }

    private void logIncomingMessage(ConsumerRecord<String, Object> record) {
        StringBuilder headersInfo = new StringBuilder();
        record.headers().forEach(header -> {
            if (header.value() != null) {
                headersInfo
                        .append(header.key())
                        .append("=")
                        .append(new String(header.value(), StandardCharsets.UTF_8))
                        .append(", ");
            }
        });
        log.debug(
                "Consuming message: topic={}, partition={}, offset={}, key={}, headers={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                headersInfo.toString());
    }

    private void validateMessage(ConsumerRecord<String, Object> record) {
        // Проверяем обязательные headers
        if (!record.headers().headers(requestIdHeader).iterator().hasNext()) {
            log.warn("Message without {} header: topic={}, key={}", requestIdHeader, record.topic(), record.key());
        }

        if (!record.headers().headers(serviceNameHeader).iterator().hasNext()) {
            log.warn("Message without {} header: topic={}, key={}", serviceNameHeader, record.topic(), record.key());
        }
    }
}
