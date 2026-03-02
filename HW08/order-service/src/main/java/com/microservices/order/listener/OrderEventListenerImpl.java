package com.microservices.order.listener;

import com.microservices.order.client.UserServiceClient;
import com.microservices.order.config.properties.SecurityProperties;
import com.microservices.order.event.OrderCreatedEvent;
import com.microservices.order.model.Order;
import com.microservices.order.publisher.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListenerImpl implements OrderEventListener {

    private final SecurityProperties securityProperties;

    private final NotificationEventPublisher notificationEventPublisher;

    private final UserServiceClient userClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Override
    public void handleEvent(OrderCreatedEvent event) {
        String requestId = event.mdcContext().get(securityProperties.getRequestIdHeader());
        MDC.clear();
        MDC.put(securityProperties.getRequestIdHeader(), requestId);

        Order order = event.order();
        log.debug("Processing event for order: {}", event);
        try {
            var userProfile = userClient.fetchMyProfile(order.getUserId());
            notificationEventPublisher.sendNotification(event.order(), userProfile, requestId);
        } catch (Exception e) {
            log.error("Error processing event: {}", event, e);
        }
        MDC.clear();
    }
}
