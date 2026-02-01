package com.microservices.billing.service;

import com.microservices.billing.kafka.PaymentEventDto;
import com.microservices.billing.kafka.PaymentEventDto.PaymentRequestEvent;
import com.microservices.billing.kafka.PaymentEventDto.PaymentResponseEvent;
import com.microservices.billing.model.Account;
import com.microservices.billing.model.Operation;
import com.microservices.billing.repository.AccountRepository;
import com.microservices.billing.repository.OperationRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OperationRepository operationRepository;

    private final AccountRepository accountRepository;

    private final AccountService accountService;

    private final AccountServiceImpl accountServiceImpl;

    @Override
    @Transactional
    public PaymentResponseEvent processPayment(PaymentRequestEvent event) {

        Optional<Operation> existingOp =
                operationRepository.findBySagaIdAndSagaStep(event.getSagaId(), Operation.SagaStep.PAYMENT);

        // Проверяем сагу - возможно она уже отменена
        if (existingOp.isPresent() && isSagaCompensated(existingOp.get())) {
            log.info(
                    "Saga {} already compensated, rejecting payment, orderId {}",
                    event.getSagaId(),
                    event.getOrderId());
        }

        if (existingOp.isPresent()) {
            Operation op = existingOp.get();
            return PaymentResponseEvent.builder()
                    .sagaId(event.getSagaId())
                    .orderId(event.getOrderId())
                    .success(op.getStatus() == Operation.Status.COMPLETED)
                    .transactionId(op.getId().toString())
                    .errorMessage(op.getStatus() == Operation.Status.FAILED ? "Payment failed previously" : null)
                    .build();
        }

        try {
            Account account = getAccount(event);
            // Создаем операцию
            Operation operation = Operation.builder()
                    .id(UUID.randomUUID())
                    .sagaId(event.getSagaId())
                    .sagaStep(Operation.SagaStep.PAYMENT)
                    .account(account)
                    .orderId(event.getOrderId())
                    .amount(event.getAmount().negate())
                    .status(Operation.Status.COMPLETED)
                    .build();
            operationRepository.save(operation);
            // Выполняем списание
            accountService.withdraw(account, event.getAmount());

            operation.setStatus(Operation.Status.COMPLETED);
            return PaymentResponseEvent.builder()
                    .sagaId(event.getSagaId())
                    .orderId(event.getOrderId())
                    .success(true)
                    .transactionId(operation.getId().toString())
                    .build();

        } catch (Exception e) {
            log.error("Payment processing failed for sagaId: {}", event.getSagaId(), e);
            return createFailedResponse(event, e.getMessage());
        }
    }

    @Override
    public PaymentResponseEvent processCompensationPayment(PaymentEventDto.OrderFailedEvent event) {
        //TODO

        Optional<Operation> existingOp =
                operationRepository.findBySagaIdAndSagaStep(event.getSagaId(), Operation.SagaStep.PAYMENT);

        // Проверяем сагу - возможно она уже отменена
        if (existingOp.isPresent() && isSagaCompensated(existingOp.get())) {
            log.info(
                    "Saga {} already compensated, rejecting payment, orderId {}",
                    event.getSagaId(),
                    event.getOrderId());
        }

        if (existingOp.isPresent()) {
            Operation op = existingOp.get();
            return PaymentResponseEvent.builder()
                    .sagaId(event.getSagaId())
                    .orderId(event.getOrderId())
                    .success(op.getStatus() == Operation.Status.COMPLETED)
                    .transactionId(op.getId().toString())
                    .errorMessage(op.getStatus() == Operation.Status.FAILED ? "Payment failed previously" : null)
                    .build();
        }

        try {
            Account account = getAccount(event);
            // Создаем операцию
            Operation operation = Operation.builder()
                    .id(UUID.randomUUID())
                    .sagaId(event.getSagaId())
                    .sagaStep(Operation.SagaStep.PAYMENT)
                    .account(account)
                    .orderId(event.getOrderId())
                    .amount(event.getAmount().negate())
                    .status(Operation.Status.COMPLETED)
                    .build();
            operationRepository.save(operation);
            // Выполняем списание
            accountService.withdraw(account, event.getAmount());

            operation.setStatus(Operation.Status.COMPLETED);
            return PaymentResponseEvent.builder()
                    .sagaId(event.getSagaId())
                    .orderId(event.getOrderId())
                    .success(true)
                    .transactionId(operation.getId().toString())
                    .build();

        } catch (Exception e) {
            log.error("Payment processing failed for sagaId: {}", event.getSagaId(), e);
            return createFailedResponse(event, e.getMessage());
        }
    }


    private Account getAccount(PaymentRequestEvent event) {
        return accountRepository
                .findByUserIdAndLock(event.getUserId())
                .orElseThrow(() -> new RuntimeException("Account not found for user: " + event.getUserId()));
    }

    private boolean isSagaCompensated(Operation sagaId) {
        return sagaId.getStatus() == Operation.Status.COMPENSATED;
    }

    private PaymentResponseEvent createFailedResponse(PaymentRequestEvent event, String error) {
        // Сохраняем информацию о неудачной операции для идемпотентности
        Operation failedOperation = Operation.builder()
                .id(UUID.randomUUID())
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .amount(event.getAmount())
                .status(Operation.Status.FAILED)
                .build();

        operationRepository.save(failedOperation);

        return PaymentResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(false)
                .errorMessage(error)
                .build();
    }
}
