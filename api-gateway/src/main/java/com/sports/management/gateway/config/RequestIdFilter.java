package com.sports.management.gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

/**
 * Gateway sinh ma theo doi X-Request-Id cho moi request (neu client chua gui) va dua vao header
 * cua request de GatewayRoutingController chuyen tiep xuong cac service phia sau.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        final String requestId = (incoming == null || incoming.length() > 64 || !incoming.matches("[A-Za-z0-9._-]+"))
                ? UUID.randomUUID().toString()
                : incoming;
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return HEADER.equalsIgnoreCase(name) ? requestId : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (HEADER.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(requestId));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = new ArrayList<>();
                Enumeration<String> original = super.getHeaderNames();
                boolean present = false;
                while (original != null && original.hasMoreElements()) {
                    String headerName = original.nextElement();
                    if (HEADER.equalsIgnoreCase(headerName)) {
                        present = true;
                    }
                    names.add(headerName);
                }
                if (!present) {
                    names.add(HEADER);
                }
                return Collections.enumeration(names);
            }
        };
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
