package com.microservices.delivery.service;

import static com.microservices.delivery.model.Reservation.SagaStep.COMPENSATION;

import com.microservices.delivery.kafka.SagaEvents.CompensationResponseEvent;
import com.microservices.delivery.kafka.SagaEvents.OrderFailedEvent;
import com.microservices.delivery.model.Reservation;
import com.microservices.delivery.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class ReservationCompensationServiceImpl implements ReservationCompensationService {

    private final ReservationRepository operationRepository;

    @Override
    @Transactional
    public CompensationResponseEvent processCompensationReservation(OrderFailedEvent event) {
        // Ищем операцию по sagaId и смотрим есть ли там платеж, может еще не провели
        Optional<Reservation> existingOp =
                operationRepository.findBySagaIdAndSagaStep(event.getSagaId(), Reservation.SagaStep.RESERVATION);

        // Нет операции - создаем компенсационную операцию, она заблокирует создание операции в будущем
        if (existingOp.isEmpty()) {
            return createNewCompensatedOperationWithoutOperation(event);
        }
        // Проверяем сагу - возможно она уже отменена
        if (isSagaCompensated(existingOp.get())) {
            return getAlreadyCompensationResponse(event);
        }
        Reservation operationReservation = existingOp.get();
        try {
            return createNewCompensatedOperation(event, operationReservation);

        } catch (Exception e) {
            log.error(" Compensation payment processing failed for sagaId: {}", event.getSagaId(), e);
            return createFailedCompensationResponse(event, e.getMessage());
        }
    }

    private CompensationResponseEvent createNewCompensatedOperationWithoutOperation(OrderFailedEvent event) {
        // Создаем операцию
        Reservation operationCompensation = getCompensationOperation(event);
        operationRepository.save(operationCompensation);
        return getCompensationResponse(event);
    }

    private static Reservation getCompensationOperation(OrderFailedEvent event) {
        return Reservation.builder()
                .sagaId(event.getSagaId())
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .expiresAt(LocalDateTime.now())
                .sagaStep(COMPENSATION)
                .status(Reservation.Status.COMPENSATED)
                .build();
    }

    private static CompensationResponseEvent getCompensationResponse(OrderFailedEvent event) {
        log.info("Saga {} compensated, orderId {}", event.getSagaId(), event.getOrderId());

        return CompensationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(true)
                .duplicate(false)
                .errorMessage("Saga compensated")
                .build();
    }

    private boolean isSagaCompensated(Reservation operation) {
        return operation.getCompensatedBy() != null;
    }

    private static CompensationResponseEvent getAlreadyCompensationResponse(OrderFailedEvent event) {
        log.info("Saga {} already compensated in previous run, orderId {}", event.getSagaId(), event.getOrderId());

        return CompensationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(true)
                .duplicate(true)
                .errorMessage("Saga already compensated in previous run")
                .build();
    }

    private CompensationResponseEvent createNewCompensatedOperation(
            OrderFailedEvent event, Reservation operationReservation) {
        // Создаем операцию
        Reservation operationCompensation = getCompensationOperation(event, operationReservation);

        operationReservation.setCompensatedBy(operationCompensation.getId());
        operationReservation.setStatus(Reservation.Status.COMPENSATED);
        operationRepository.save(operationCompensation);
        operationRepository.save(operationReservation);
        return getCompensationResponse(event);
    }

    private static Reservation getCompensationOperation(OrderFailedEvent event, Reservation operationReservation) {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .sagaId(event.getSagaId())
                .sagaStep(COMPENSATION)
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .expiresAt(LocalDateTime.now())
                .status(Reservation.Status.COMPENSATED)
                .compensates((operationReservation.getId()))
                .build();
    }

    private CompensationResponseEvent createFailedCompensationResponse(OrderFailedEvent event, String message) {
        return CompensationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(false)
                .errorMessage(message)
                .build();
    }
}
