package com.microservices.warehouse.service;

import static com.microservices.warehouse.model.Reservation.SagaStep.RESERVATION;

import com.microservices.warehouse.kafka.SagaEvents.WarehouseReservationRequestEvent;
import com.microservices.warehouse.kafka.SagaEvents.WarehouseReservationResponseEvent;
import com.microservices.warehouse.model.Catalog;
import com.microservices.warehouse.model.Reservation;
import com.microservices.warehouse.model.ReservationItem;
import com.microservices.warehouse.repository.CatalogRepository;
import com.microservices.warehouse.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;

    private final CatalogRepository catalogRepository;

    @Override
    @Transactional
    public WarehouseReservationResponseEvent processReservation(WarehouseReservationRequestEvent event) {
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
            return createNewReservation(event);
        } catch (Exception e) {
            log.error("Reservation processing failed for sagaId: {}", event.getSagaId(), e);
            return createFailedReservation(event, e.getMessage());
        }
    }

    private WarehouseReservationResponseEvent createFailedReservation(
            WarehouseReservationRequestEvent event, String error) {

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

        return WarehouseReservationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(false)
                .errorMessage(error)
                .build();
    }

    private WarehouseReservationResponseEvent createNewReservation(WarehouseReservationRequestEvent event) {
        List<Catalog> catalogItems = getCatalogItems(event.getItems());

        Map<UUID, Integer> itemsRequest = event.getItems().stream()
                .collect(Collectors.toMap(
                        WarehouseReservationRequestEvent.ReservationItem::getCatalogItemId,
                        WarehouseReservationRequestEvent.ReservationItem::getQuantity));
        // Создаем операцию
        Reservation operation = prepareReservation(event);
        // Создаем элеенты резервирования
        List<ReservationItem> reservationItems = prepareItems(catalogItems, operation, itemsRequest);
        operation.setItems(reservationItems);
        // Проводим списание с каталога
        catalogItems.forEach(item -> {
            item.setQuantity(item.getQuantity() - itemsRequest.get(item.getId()));
            catalogRepository.save(item);
        });
        reservationRepository.save(operation);
        return WarehouseReservationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(true)
                .reservationId(operation.getId())
                .build();
    }

    private static List<ReservationItem> prepareItems(
            List<Catalog> catalogItems, Reservation operation, Map<UUID, Integer> itemsRequest) {
        return catalogItems.stream()
                .map(catalogItem -> ReservationItem.builder()
                        .id(UUID.randomUUID())
                        .reservation(operation)
                        .catalog(catalogItem)
                        .quantity(itemsRequest.get(catalogItem.getId()))
                        .build())
                .toList();
    }

    private static Reservation prepareReservation(WarehouseReservationRequestEvent event) {
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

    private List<Catalog> getCatalogItems(List<WarehouseReservationRequestEvent.ReservationItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Reservation items cannot be empty");
        }
        List<Catalog> catalogItems = new ArrayList<>();
        for (WarehouseReservationRequestEvent.ReservationItem item : items) {
            // Используем метод с блокировкой
            Catalog catalogItem = catalogRepository
                    .findByIdWithLock(item.getCatalogItemId())
                    .orElseThrow(() -> new ItemReservationException("Item not found: " + item.getCatalogItemId()));
            if (catalogItem.getQuantity() < item.getQuantity()) {
                throw new ItemReservationException(String.format(
                        "Not enough items in stock: %s. Available: %d, requested: %d",
                        item.getCatalogItemId(), catalogItem.getQuantity(), item.getQuantity()));
            }
            catalogItems.add(catalogItem);
        }

        return catalogItems;
    }

    // Делаем новое исключение на элементы
    public static class ItemReservationException extends RuntimeException {
        public ItemReservationException(String message) {
            super(message);
        }
    }

    private static WarehouseReservationResponseEvent getAlreadyCompensatedReservation(
            WarehouseReservationRequestEvent event) {
        log.info(
                "Saga {} already compensated, rejecting reservation for orderId {}",
                event.getSagaId(),
                event.getOrderId());

        return WarehouseReservationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(false)
                .errorMessage("Saga already compensated")
                .build();
    }

    private static WarehouseReservationResponseEvent getAlreadyDoneReservation(
            WarehouseReservationRequestEvent event, Reservation op) {
        return WarehouseReservationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(op.getStatus() == Reservation.Status.COMPLETED)
                .reservationId(op.getId())
                .errorMessage(op.getStatus() == Reservation.Status.FAILED ? "Reservation failed previously" : null)
                .build();
    }
}
