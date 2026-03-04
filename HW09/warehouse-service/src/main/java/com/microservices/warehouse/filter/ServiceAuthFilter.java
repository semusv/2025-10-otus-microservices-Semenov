package com.microservices.warehouse.filter;

import com.microservices.warehouse.config.properties.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class ServiceAuthFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        // Обрабатываем ТОЛЬКО внутренние эндпоинты
        if (!path.startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String serviceSecret = request.getHeader(securityProperties.getServiceSecretHeader());
        String serviceName = request.getHeader(securityProperties.getServiceNameHeader());
        if (serviceSecret == null || !serviceSecret.equals(securityProperties.getSecret())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Invalid service secret");
            return;
        }
        if (serviceName == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Empty service name");
            return;
        }
        // Аутентифицируем как сервис
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                serviceName, null, List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
