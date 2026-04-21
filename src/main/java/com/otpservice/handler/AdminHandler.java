package com.otpservice.handler;

import com.otpservice.model.OtpConfig;
import com.otpservice.model.User;
import com.otpservice.repository.OtpConfigRepository;
import com.otpservice.repository.UserRepository;
import com.otpservice.util.HttpUtil;
import com.otpservice.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AdminHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(AdminHandler.class);

    private final UserRepository userRepository;
    private final OtpConfigRepository otpConfigRepository;

    public AdminHandler(UserRepository userRepository, OtpConfigRepository otpConfigRepository) {
        this.userRepository = userRepository;
        this.otpConfigRepository = otpConfigRepository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("PUT".equals(method) && "/admin/config".equals(path)) {
                handleUpdateConfig(exchange);
            } else if ("GET".equals(method) && "/admin/users".equals(path)) {
                handleGetUsers(exchange);
            } else if ("DELETE".equals(method) && path.startsWith("/admin/users/")) {
                handleDeleteUser(exchange, path);
            } else {
                HttpUtil.sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            log.error("Unhandled error in AdminHandler", e);
            HttpUtil.sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleUpdateConfig(HttpExchange exchange) throws IOException, SQLException {
        String body = HttpUtil.readBody(exchange);
        try {
            UpdateConfigRequest req = JsonUtil.fromJson(body, UpdateConfigRequest.class);
            if (req.codeLength() == null || req.ttlSeconds() == null) {
                HttpUtil.sendError(exchange, 400, "codeLength and ttlSeconds are required");
                return;
            }
            if (req.codeLength() < 4 || req.codeLength() > 10) {
                HttpUtil.sendError(exchange, 400, "codeLength must be between 4 and 10");
                return;
            }
            if (req.ttlSeconds() <= 0) {
                HttpUtil.sendError(exchange, 400, "ttlSeconds must be positive");
                return;
            }
            otpConfigRepository.updateConfig(req.codeLength(), req.ttlSeconds());
            OtpConfig updated = otpConfigRepository.findConfig();
            HttpUtil.sendJson(exchange, 200, updated);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("JSON")) {
                HttpUtil.sendError(exchange, 400, "Invalid JSON: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private void handleGetUsers(HttpExchange exchange) throws IOException, SQLException {
        List<User> users = userRepository.findAllNonAdmins();
        HttpUtil.sendJson(exchange, 200, users);
    }

    private void handleDeleteUser(HttpExchange exchange, String path) throws IOException, SQLException {
        String idStr = HttpUtil.extractPathVar(path, "/admin/users/");
        if (idStr == null || idStr.isBlank()) {
            HttpUtil.sendError(exchange, 400, "Missing user id");
            return;
        }
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            HttpUtil.sendError(exchange, 400, "Invalid user id");
            return;
        }
        boolean deleted = userRepository.deleteById(id);
        if (deleted) {
            exchange.sendResponseHeaders(204, -1);
        } else {
            HttpUtil.sendError(exchange, 404, "User not found or is an admin");
        }
    }

    public record UpdateConfigRequest(Integer codeLength, Integer ttlSeconds) {}
}
