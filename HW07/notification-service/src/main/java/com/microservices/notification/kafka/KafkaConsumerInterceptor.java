package com.microservices.notification.kafka;

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
        offsets.forEach((tp, offset) -> log.debug(
                "Committed offset {} for topic {} partition {}", offset.offset(), tp.topic(), tp.partition()));
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
    }

    private void setupContextFromHeaders(ConsumerRecord<String, Object> recordEvent) {
        MDC.clear();
        recordEvent
                .headers()
                .headers(KafkaCustomHeaders.REQUEST_ID)
                .forEach(header ->
                        MDC.put(KafkaCustomHeaders.REQUEST_ID, new String(header.value(), StandardCharsets.UTF_8)));
    }

    private void logIncomingMessage(ConsumerRecord<String, Object> recordEvent) {
        StringBuilder headersInfo = new StringBuilder();
        recordEvent.headers().forEach(header -> {
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
                recordEvent.topic(),
                recordEvent.partition(),
                recordEvent.offset(),
                recordEvent.key(),
                headersInfo);
    }

    private void validateMessage(ConsumerRecord<String, Object> recordEvent) {
        // Проверяем обязательные headers
        if (!recordEvent
                .headers()
                .headers(KafkaCustomHeaders.REQUEST_ID)
                .iterator()
                .hasNext()) {
            log.warn(
                    "Message without {} header: topic={}, key={}",
                    KafkaCustomHeaders.REQUEST_ID,
                    recordEvent.topic(),
                    recordEvent.key());
        }

        if (!recordEvent.headers().headers(KafkaCustomHeaders.SOURCE).iterator().hasNext()) {
            log.warn(
                    "Message without {} header: topic={}, key={}",
                    KafkaCustomHeaders.SOURCE,
                    recordEvent.topic(),
                    recordEvent.key());
        }
    }
}
