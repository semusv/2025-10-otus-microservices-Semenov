package com.microservices.billing.service;

import static com.microservices.billing.model.Operation.SagaStep.PAYMENT;

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

        Optional<Operation> existingOp = operationRepository.findBySagaIdAndSagaStep(event.getSagaId(), PAYMENT);
        // Проверяем сагу - возможно она уже отменена
        if (existingOp.isPresent() && isSagaCompensated(existingOp.get())) {
            return getAlreadyCompensatedPaymentResponse(event);
        }
        // Проверяем, что операция уже выполнена
        if (existingOp.isPresent()) {
            return getAlreadyDonePaymentResponseEvent(event, existingOp.get());
        }
        // Создаем новую операцию
        try {
            return createNewPaymentOperation(event);
        } catch (Exception e) {
            log.error("Payment processing failed for sagaId: {}", event.getSagaId(), e);
            return createFailedResponse(event, e.getMessage());
        }
    }

    private PaymentResponseEvent createNewPaymentOperation(PaymentRequestEvent event) {
        Account account = getAccount(event.getUserId());
        // Создаем операцию
        Operation operation = Operation.builder()
                .id(UUID.randomUUID())
                .sagaId(event.getSagaId())
                .sagaStep(PAYMENT)
                .account(account)
                .orderId(event.getOrderId())
                .amount(event.getAmount().negate())
                .status(Operation.Status.COMPLETED)
                .build();
        // Выполняем списание
        accountService.withdraw(account, event.getAmount());

        operationRepository.save(operation);

        return PaymentResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(true)
                .transactionId(operation.getId().toString())
                .build();
    }

    private static PaymentResponseEvent getAlreadyDonePaymentResponseEvent(PaymentRequestEvent event, Operation op) {
        return PaymentResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(op.getStatus() == Operation.Status.COMPLETED)
                .transactionId(op.getId().toString())
                .errorMessage(op.getStatus() == Operation.Status.FAILED ? "Payment failed previously" : null)
                .build();
    }

    private static PaymentResponseEvent getAlreadyCompensatedPaymentResponse(PaymentRequestEvent event) {
        log.info("Saga {} already compensated, rejecting payment, orderId {}", event.getSagaId(), event.getOrderId());

        return PaymentResponseEvent.builder()
                .sagaId(event.getSagaId())
                .orderId(event.getOrderId())
                .success(false)
                .errorMessage("Saga already compensated")
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
