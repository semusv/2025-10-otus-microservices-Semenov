package com.microservices.billing.service;

import static com.microservices.billing.model.Operation.SagaStep.COMPENSATION;
import static com.microservices.billing.model.Operation.Status.COMPENSATED;

import com.microservices.billing.kafka.PaymentEventDto.CompensationResponseEvent;
import com.microservices.billing.kafka.PaymentEventDto.OrderFailedEvent;
import com.microservices.billing.model.Account;
import com.microservices.billing.model.Operation;
import com.microservices.billing.repository.AccountRepository;
import com.microservices.billing.repository.OperationRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentCompensationServiceImpl implements PaymentCompensationService {

    private final OperationRepository operationRepository;

    private final AccountRepository accountRepository;

    private final AccountService accountService;

    @Override
    @Transactional
    public CompensationResponseEvent processCompensationPayment(OrderFailedEvent event) {
        // Ищем операцию по sagaId и смотрим есть ли там платеж, может еще не провели
        Optional<Operation> existingOp =
                operationRepository.findBySagaIdAndSagaStep(event.getSagaId(), Operation.SagaStep.PAYMENT);

        // Нет платежа - создаем компенсационную операцию, она заблокирует создание операции в будущем
        if (existingOp.isEmpty()) {
            return createNewCompensatedOperationWithoutOperation(event);
        }
        // Проверяем сагу - возможно она уже отменена
        if (isSagaCompensated(existingOp.get())) {
            return getAlreadyCompensationResponse(event);
        }
        Operation operationPayment = existingOp.get();
        try {
            return createNewCompensatedOperation(event, operationPayment);

        } catch (Exception e) {
            log.error(" Compensation payment processing failed for sagaId: {}", event.getSagaId(), e);
            return createFailedCompensationResponse(event, e.getMessage());
        }
    }

    private CompensationResponseEvent createNewCompensatedOperationWithoutOperation(OrderFailedEvent event) {
        Account account = getAccount(event.getUserId());
        // Создаем операцию
        Operation operationCompensation = getCompensationOperation(event, account);
        operationRepository.save(operationCompensation);
        return getCompensationResponse(event);
    }

    private CompensationResponseEvent createNewCompensatedOperation(
            OrderFailedEvent event, Operation operationPayment) {
        Account account = getAccount(event.getUserId());
        // Создаем операцию
        Operation operationCompensation = getCompensationOperation(event, account, operationPayment);
        // Выполняем пополнение
        accountService.deposit(account, operationPayment.getAmount());

        operationPayment.setCompensatedBy(operationCompensation.getId());
        operationPayment.setStatus(COMPENSATED);
        operationRepository.save(operationCompensation);
        operationRepository.save(operationPayment);
        return getCompensationResponse(event);
    }

    private CompensationResponseEvent createFailedCompensationResponse(OrderFailedEvent event, String message) {
        return CompensationResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(false)
                .errorMessage(message)
                .build();
    }

    private static Operation getCompensationOperation(OrderFailedEvent event, Account account) {
        return Operation.builder()
                .id(UUID.randomUUID())
                .sagaId(event.getSagaId())
                .sagaStep(COMPENSATION)
                .account(account)
                .orderId(event.getOrderId())
                .amount(new BigDecimal(0))
                .status(COMPENSATED)
                .build();
    }

    private static Operation getCompensationOperation(
            OrderFailedEvent event, Account account, Operation operationPayment) {
        return Operation.builder()
                .id(UUID.randomUUID())
                .sagaId(event.getSagaId())
                .sagaStep(COMPENSATION)
                .account(account)
                .orderId(event.getOrderId())
                .amount(operationPayment.getAmount())
                .status(COMPENSATED)
                .compensates((operationPayment.getId()))
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

    private Account getAccount(UUID userId) {
        return accountRepository
                .findByUserIdAndLock(userId)
                .orElseThrow(() -> new RuntimeException("Account not found for user: " + userId));
    }

    private boolean isSagaCompensated(Operation operation) {
        return operation.getCompensatedBy() != null;
    }
}
