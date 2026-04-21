package com.otpservice.handler;

import com.otpservice.service.OtpService;
import com.otpservice.util.HttpUtil;
import com.otpservice.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class OtpHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(OtpHandler.class);

    private final OtpService otpService;

    public OtpHandler(OtpService otpService) {
        this.otpService = otpService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        Claims claims = (Claims) exchange.getAttribute("claims");
        long userId = Long.parseLong(claims.getSubject());

        try {
            if ("POST".equals(method) && "/otp/generate".equals(path)) {
                handleGenerate(exchange, userId);
            } else if ("POST".equals(method) && "/otp/validate".equals(path)) {
                handleValidate(exchange, userId);
            } else {
                HttpUtil.sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            log.error("Unhandled error in OtpHandler", e);
            HttpUtil.sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleGenerate(HttpExchange exchange, long userId) throws IOException {
        String body = HttpUtil.readBody(exchange);
        try {
            GenerateRequest req = JsonUtil.fromJson(body, GenerateRequest.class);
            if (req.operationId() == null || req.operationId().isBlank()) {
                HttpUtil.sendError(exchange, 400, "operationId is required");
                return;
            }
            if (req.channel() == null || req.channel().isBlank()) {
                HttpUtil.sendError(exchange, 400, "channel is required");
                return;
            }
            otpService.generateOtp(userId, req.operationId(), req.channel(),
                    req.destination() != null ? req.destination() : "");
            HttpUtil.sendJson(exchange, 200, Map.of("message", "OTP sent successfully"));
        } catch (IllegalArgumentException e) {
            HttpUtil.sendError(exchange, 400, e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("JSON")) {
                HttpUtil.sendError(exchange, 400, "Invalid JSON: " + e.getMessage());
            } else {
                throw new IOException(e);
            }
        } catch (Exception e) {
            log.error("Error in handleGenerate", e);
            HttpUtil.sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleValidate(HttpExchange exchange, long userId) throws IOException {
        String body = HttpUtil.readBody(exchange);
        try {
            ValidateRequest req = JsonUtil.fromJson(body, ValidateRequest.class);
            if (req.operationId() == null || req.operationId().isBlank()) {
                HttpUtil.sendError(exchange, 400, "operationId is required");
                return;
            }
            if (req.code() == null || req.code().isBlank()) {
                HttpUtil.sendError(exchange, 400, "code is required");
                return;
            }
            boolean valid = otpService.validateOtp(userId, req.operationId(), req.code());
            if (valid) {
                HttpUtil.sendJson(exchange, 200, Map.of("valid", true));
            } else {
                HttpUtil.sendJson(exchange, 400, Map.of("valid", false, "error", "Invalid or expired OTP"));
            }
        } catch (IllegalArgumentException e) {
            HttpUtil.sendError(exchange, 400, e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("JSON")) {
                HttpUtil.sendError(exchange, 400, "Invalid JSON: " + e.getMessage());
            } else {
                throw new IOException(e);
            }
        } catch (Exception e) {
            log.error("Error in handleValidate", e);
            HttpUtil.sendError(exchange, 500, "Internal server error");
        }
    }

    public record GenerateRequest(String operationId, String channel, String destination) {}
    public record ValidateRequest(String operationId, String code) {}
}
