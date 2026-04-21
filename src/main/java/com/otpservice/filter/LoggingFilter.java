package com.otpservice.filter;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoggingFilter extends Filter {
    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        long start = System.currentTimeMillis();
        chain.doFilter(exchange);
        long elapsed = System.currentTimeMillis() - start;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        int status = exchange.getResponseCode();
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        Claims claims = (Claims) exchange.getAttribute("claims");
        String user = (claims != null) ? claims.get("login", String.class) : "anonymous";
        long responseSize = exchange.getResponseHeaders().containsKey("Content-length")
                ? Long.parseLong(exchange.getResponseHeaders().getFirst("Content-length"))
                : -1;

        if (status >= 500) {
            log.error("{} {} -> {} | ip={} user={} size={}b | {}ms",
                    method, path, status, ip, user, responseSize, elapsed);
        } else if (status >= 400) {
            log.warn("{} {} -> {} | ip={} user={} size={}b | {}ms",
                    method, path, status, ip, user, responseSize, elapsed);
        } else {
            log.info("{} {} -> {} | ip={} user={} size={}b | {}ms",
                    method, path, status, ip, user, responseSize, elapsed);
        }
    }

    @Override
    public String description() {
        return "Request Logging Filter";
    }
}
