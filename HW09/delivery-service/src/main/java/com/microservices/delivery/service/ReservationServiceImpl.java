package com.microservices.delivery.service;

import static com.microservices.delivery.model.Reservation.SagaStep.RESERVATION;

import com.microservices.delivery.kafka.SagaEvents.DeliveryReservationRequestEvent;
import com.microservices.delivery.kafka.SagaEvents.DeliveryReservationResponseEvent;
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
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    public DeliveryReservationResponseEvent processReservation(DeliveryReservationRequestEvent event) {
        Optional<Reservation> existingOp =
                reservationRepository.findBySagaIdAndSagaStep(event.getSagaId(), RESERVATION);
        Optional<Reservation> existingOpComps =
                reservationRepository.findBySagaIdAndSagaStep(event.getSagaId(), Reservation.SagaStep.COMPENSATION);
        // Проверяем сагу - возможно она уже отменена
        if (existingOpComps.isPresent()) {
            return getAlreadyCompensatedReservation(event);
        }
        // Проверяем, что операция уже выполнена
        if (existingOp.isPresent()) {
            return getAlreadyDoneReservation(event, existingOp.get());
        }
        // Создаем новое резервирование
        try {
            // Эмитация запроса у внешней службы
            //            if (Math.random() < 0.5) {
            //                throw new ItemReservationException("External delivery service send error");
            //            }
            return createNewReservation(event);
        } catch (Exception e) {
            log.error("Reservation processing failed for sagaId: {}", event.getSagaId(), e);
            return createFailedReservation(event, e.getMessage());
        }
    }

    private DeliveryReservationResponseEvent createFailedReservation(
            DeliveryReservationRequestEvent event, String error) {

        Reservation failedOperation = Reservation.builder()
                .id(UUID.randomUUID())
                .sagaId(event.getSagaId())
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .sagaStep(Reservation.SagaStep.COMPENSATION)
                .expiresAt(LocalDateTime.now())
                .status(Reservation.Status.FAILED)
                .build();

        reservationRepository.save(failedOperation);

        return DeliveryReservationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(false)
                .errorMessage(error)
                .build();
    }

    private DeliveryReservationResponseEvent createNewReservation(DeliveryReservationRequestEvent event) {

        // Создаем операцию
        Reservation operation = prepareReservation(event);

        reservationRepository.save(operation);
        return DeliveryReservationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(true)
                .reservationId(operation.getId())
                .build();
    }

    private static Reservation prepareReservation(DeliveryReservationRequestEvent event) {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .sagaId(event.getSagaId())
                .sagaStep(RESERVATION)
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .status(Reservation.Status.COMPLETED)
                .build();
    }

    private static DeliveryReservationResponseEvent getAlreadyCompensatedReservation(
            DeliveryReservationRequestEvent event) {
        log.info(
                "Saga {} already compensated, rejecting reservation for orderId {}",
                event.getSagaId(),
                event.getOrderId());

        return DeliveryReservationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(false)
                .errorMessage("Saga already compensated")
                .build();
    }

    private static DeliveryReservationResponseEvent getAlreadyDoneReservation(
            DeliveryReservationRequestEvent event, Reservation op) {
        return DeliveryReservationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(op.getStatus() == Reservation.Status.COMPLETED)
                .reservationId(op.getId())
                .errorMessage(op.getStatus() == Reservation.Status.FAILED ? "Reservation failed previously" : null)
                .build();
    }
}
