package com.microservices.order.filter;

import com.microservices.order.config.properties.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class MdcFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;

    private final Logger log = LoggerFactory.getLogger(MdcFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        var xRequestId = request.getHeader(securityProperties.getRequestIdHeader());
        log.debug("xRequestId:{}", xRequestId);
        if (xRequestId != null) {
            MDC.put(securityProperties.getRequestIdHeader(), xRequestId);
        }

        var headerIterator = request.getHeaderNames().asIterator();
        var headers = new ArrayList<String>();
        while (headerIterator.hasNext()) {
            headers.add(headerIterator.next());
        }

        log.debug("request headers:{}", headers);
        response.addHeader(securityProperties.getRequestIdHeader(), xRequestId);

        filterChain.doFilter(request, response);

        MDC.remove(securityProperties.getRequestIdHeader());
        log.debug("response headers:{}", response.getHeaderNames());
    }
}
