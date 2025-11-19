package com.ragchat.chat.logging;

import com.ragchat.chat.security.ChatUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = getOrCreateCorrelationId(request);

        try {
            MDC.put("correlationId", correlationId);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof ChatUserPrincipal principal) {
                if (principal.getUserId() != null) {
                    MDC.put("userId", principal.getUserId().toString());
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("userId");
        }
    }

    private String getOrCreateCorrelationId(HttpServletRequest request) {
        String header = request.getHeader("X-Correlation-Id");
        if (header != null && !header.isBlank()) {
            return header;
        }
        return UUID.randomUUID().toString();
    }
}
