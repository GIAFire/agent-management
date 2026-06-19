package org.zw;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestMdcFilter extends OncePerRequestFilter {

    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_CONVERSATION_ID = "conversationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain ) throws ServletException, IOException {

        String requestId = firstNonBlank(
                request.getHeader("X-Request-Id"),
                UUID.randomUUID().toString());

        String tenantId = firstNonBlank(
                request.getHeader("X-Tenant-Id"),
                "default");

        String conversationId = request.getHeader("X-Conversation-Id");

        String userId = firstNonBlank(
                resolveUserId(),
                "anonymous");

        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_TENANT_ID, tenantId);
        MDC.put(MDC_USER_ID, userId);
        MDC.put(MDC_CONVERSATION_ID, conversationId);

        response.setHeader("X-Request-Id", requestId);
        response.setHeader("X-Conversation-Id", conversationId);

        try {
            filterChain.doFilter(request, response);
        }
        finally {
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CONVERSATION_ID);
        }
    }

    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getName();
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}