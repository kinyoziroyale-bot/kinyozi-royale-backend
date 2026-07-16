package com.kinyozi.royale.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * V5 (new): assigns a request-id to every incoming HTTP request, puts it in
 * SLF4J MDC ({@code requestId}), and echoes it back via the
 * {@code X-Request-Id} response header. Existing well-formed inbound
 * X-Request-Id headers are honoured so the frontend can propagate the id
 * end-to-end; anything malformed is replaced with a fresh UUID to prevent
 * log-forging.
 *
 * Runs BEFORE the JWT filters so audit logs emitted by them include the id.
 * Include {@code %X{requestId}} in your logback pattern to surface it in logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    // Allow 8-64 chars of URL-safe id; reject anything else.
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String inbound = req.getHeader(HEADER);
        String id = (inbound != null && SAFE_ID.matcher(inbound).matches())
                ? inbound
                : UUID.randomUUID().toString();
        MDC.put(MDC_KEY, id);
        res.setHeader(HEADER, id);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
