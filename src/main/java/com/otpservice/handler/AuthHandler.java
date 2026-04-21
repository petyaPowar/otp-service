package com.otpservice.handler;

import com.otpservice.model.User;
import com.otpservice.service.AuthService;
import com.otpservice.util.HttpUtil;
import com.otpservice.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class AuthHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);

    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("POST".equals(method) && "/auth/register".equals(path)) {
                handleRegister(exchange);
            } else if ("POST".equals(method) && "/auth/login".equals(path)) {
                handleLogin(exchange);
            } else {
                HttpUtil.sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            log.error("Unhandled error in AuthHandler", e);
            HttpUtil.sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = HttpUtil.readBody(exchange);
        try {
            RegisterRequest req = JsonUtil.fromJson(body, RegisterRequest.class);
            User user = authService.register(req.login(), req.password(), req.role());
            HttpUtil.sendJson(exchange, 201, Map.of(
                    "id", user.getId(),
                    "login", user.getLogin(),
                    "role", user.getRole()
            ));
        } catch (IllegalArgumentException e) {
            HttpUtil.sendError(exchange, 400, e.getMessage());
        } catch (IllegalStateException e) {
            HttpUtil.sendError(exchange, 409, e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("JSON")) {
                HttpUtil.sendError(exchange, 400, "Invalid JSON: " + e.getMessage());
            } else {
                throw e;
            }
        } catch (Exception e) {
            log.error("Error in handleRegister", e);
            HttpUtil.sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = HttpUtil.readBody(exchange);
        try {
            LoginRequest req = JsonUtil.fromJson(body, LoginRequest.class);
            String token = authService.login(req.login(), req.password());
            HttpUtil.sendJson(exchange, 200, Map.of("token", token));
        } catch (IllegalArgumentException e) {
            HttpUtil.sendError(exchange, 401, e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("JSON")) {
                HttpUtil.sendError(exchange, 400, "Invalid JSON: " + e.getMessage());
            } else {
                throw e;
            }
        } catch (Exception e) {
            log.error("Error in handleLogin", e);
            HttpUtil.sendError(exchange, 500, "Internal server error");
        }
    }

    public record RegisterRequest(String login, String password, String role) {}
    public record LoginRequest(String login, String password) {}
}
