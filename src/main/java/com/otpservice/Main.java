package com.otpservice;

import com.otpservice.config.AppConfig;
import com.otpservice.db.DatabaseManager;
import com.otpservice.filter.JwtAuthFilter;
import com.otpservice.filter.LoggingFilter;
import com.otpservice.handler.AdminHandler;
import com.otpservice.handler.AuthHandler;
import com.otpservice.handler.OtpHandler;
import com.otpservice.repository.OtpCodeRepository;
import com.otpservice.repository.OtpConfigRepository;
import com.otpservice.repository.UserRepository;
import com.otpservice.service.AuthService;
import com.otpservice.service.OtpExpiryScheduler;
import com.otpservice.service.OtpService;
import com.otpservice.service.notification.*;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.getInstance();

        DatabaseManager.getInstance().initializeSchema();

        UserRepository userRepository = new UserRepository();
        OtpConfigRepository otpConfigRepository = new OtpConfigRepository();
        OtpCodeRepository otpCodeRepository = new OtpCodeRepository();

        Map<String, NotificationChannel> channels = Map.of(
                "EMAIL",    new EmailNotificationService(),
                "SMS",      new SmsNotificationService(),
                "TELEGRAM", new TelegramNotificationService(),
                "FILE",     new FileNotificationService()
        );

        AuthService authService = new AuthService(userRepository);
        OtpService otpService = new OtpService(otpCodeRepository, otpConfigRepository, channels);

        AuthHandler authHandler = new AuthHandler(authService);
        AdminHandler adminHandler = new AdminHandler(userRepository, otpConfigRepository);
        OtpHandler otpHandler = new OtpHandler(otpService);

        int port = config.getServerPort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(config.getThreadPoolSize()));

        LoggingFilter loggingFilter = new LoggingFilter();

        HttpContext authCtx = server.createContext("/auth", authHandler);
        authCtx.getFilters().add(loggingFilter);

        HttpContext adminCtx = server.createContext("/admin", adminHandler);
        adminCtx.getFilters().add(loggingFilter);
        adminCtx.getFilters().add(new JwtAuthFilter("ADMIN"));

        HttpContext otpCtx = server.createContext("/otp", otpHandler);
        otpCtx.getFilters().add(loggingFilter);
        otpCtx.getFilters().add(new JwtAuthFilter("USER"));

        OtpExpiryScheduler scheduler = new OtpExpiryScheduler(otpCodeRepository);
        scheduler.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            scheduler.stop();
            server.stop(5);
        }));

        server.start();
        log.info("OTP Service started on port {}", port);
    }
}
