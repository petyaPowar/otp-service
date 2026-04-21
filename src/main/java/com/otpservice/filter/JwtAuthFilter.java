package com.otpservice.filter;

import com.otpservice.util.HttpUtil;
import com.otpservice.util.JwtUtil;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JwtAuthFilter extends Filter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final String requiredRole;

    public JwtAuthFilter(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String token = HttpUtil.extractBearerToken(exchange);
        if (token == null) {
            HttpUtil.sendError(exchange, 401, "Missing Authorization header");
            return;
        }
        try {
            Claims claims = JwtUtil.parseToken(token);
            if (requiredRole != null) {
                String role = claims.get("role", String.class);
                if (!requiredRole.equals(role)) {
                    HttpUtil.sendError(exchange, 403, "Insufficient permissions");
                    return;
                }
            }
            exchange.setAttribute("claims", claims);
            chain.doFilter(exchange);
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            HttpUtil.sendError(exchange, 401, "Invalid or expired token");
        }
    }

    @Override
    public String description() {
        return "JWT Authentication Filter" + (requiredRole != null ? " [" + requiredRole + "]" : "");
    }
}
