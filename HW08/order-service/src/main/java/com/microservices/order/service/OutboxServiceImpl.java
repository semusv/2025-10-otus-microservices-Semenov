package com.microservices.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.order.model.EventType;
import com.microservices.order.model.OutboxEvent;
import com.microservices.order.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxServiceImpl implements OutboxService {
    private final OutboxEventRepository outboxRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    @Value("${outbox.max-retries:3}")
    private int maxRetries;

    @Value("${outbox.batch-size:50}")
    private int batchSize;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void saveEvent(EventType eventType, String aggregateId, String aggregateType, Object payload, String topic) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setEventId(java.util.UUID.randomUUID().toString());
            event.setEventType(eventType);
            event.setAggregateId(aggregateId);
            event.setAggregateType(aggregateType);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setTopic(topic);
            event.setCreatedAt(LocalDateTime.now());

            outboxRepository.save(event);
            log.debug("Saved outbox event: {} for {}", eventType, aggregateId);
        } catch (Exception e) {
            log.error("Failed to save outbox event", e);
            throw new OutboxEventSaveException("Failed to save outbox event", e);
        }
    }

    @Scheduled(fixedDelayString = "${app.outbox.polling-interval:5000}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository.findUnpublishedEvents(maxRetries);
        for (OutboxEvent event : events) {
            try {
                CompletableFuture<?> future =
                        kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload());
                future.get(10, java.util.concurrent.TimeUnit.SECONDS);
                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
                event.setErrorMessage(null);
                outboxRepository.save(event);

                log.info("Published event {} to topic {}", event.getEventType(), event.getTopic());
            } catch (InterruptedException interruptedException) {
                errorHandle(event, interruptedException);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                errorHandle(event, e);
            }
        }
    }

    private void errorHandle(OutboxEvent event, Exception e) {
        log.error("Failed to publish event {}: {}", event.getEventId(), e.getMessage());

        outboxRepository.incrementRetryCount(event.getEventId(), e.getMessage());

        if (event.getRetryCount() >= maxRetries) {
            log.error("Event {} exceeded max retries, moving to DLQ", event.getEventId());
            moveToDlq(event);
        }
    }

    private void moveToDlq(OutboxEvent event) {
        // Можно отправить в DLQ топик или сохранить в отдельную таблицу
        log.error(
                "DLQ: Event {} failed after {} retries: {}",
                event.getEventId(),
                event.getRetryCount(),
                event.getErrorMessage());
    }

    public static class OutboxEventSaveException extends RuntimeException {
        public OutboxEventSaveException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
