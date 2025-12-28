package com.microservices.gateway.filter;

import com.microservices.gateway.config.properties.SecurityProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class XrequestFilter implements GlobalFilter, Ordered {

    private final SecurityProperties securityProperties;

    private final Logger log = LoggerFactory.getLogger(XrequestFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. Получаем или генерируем RequestId
        String requestId = exchange.getRequest().getHeaders().getFirst(securityProperties.getRequestIdHeader());

        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        // 2. Логируем (важно делать это на самом раннем этапе)
        log.info(
                "RequestId: {}, Method: {}, Path: {}",
                requestId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath());

        // 3. Добавляем RequestId в заголовки запроса (для downstream сервисов)
        var mutatedRequest = exchange.getRequest()
                .mutate()
                .header(securityProperties.getRequestIdHeader(), requestId)
                .build();

        //        // 4. Добавляем RequestId в заголовки ответа (клиенту)
        //        exchange.getResponse().getHeaders().add(securityProperties.getRequestIdHeader(), requestId);

        // 5. Продолжаем цепочку фильтров с сохранением в контексте
        String finalRequestId = requestId;
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(context -> context.put(securityProperties.getRequestIdHeader(), finalRequestId));

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
