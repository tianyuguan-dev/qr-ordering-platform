package com.qrordering.auth.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * For GET /sse/subscribe/*, copy query param "token" to Authorization header
 * so EventSource (which cannot set headers) can authenticate via ?token=...
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SseTokenQueryFilter extends OncePerRequestFilter implements Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String uri = request.getRequestURI();
        if (uri == null || !uri.contains("/sse/subscribe")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = request.getParameter("token");
        if (!StringUtils.hasText(token) || request.getHeader("Authorization") != null) {
            filterChain.doFilter(request, response);
            return;
        }
        HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("Authorization".equalsIgnoreCase(name)) {
                    return "Bearer " + token.trim();
                }
                return super.getHeader(name);
            }
            @Override
            public java.util.Enumeration<String> getHeaders(String name) {
                if ("Authorization".equalsIgnoreCase(name)) {
                    return Collections.enumeration(Collections.singletonList("Bearer " + token.trim()));
                }
                return super.getHeaders(name);
            }
        };
        filterChain.doFilter(wrapped, response);
    }
}
