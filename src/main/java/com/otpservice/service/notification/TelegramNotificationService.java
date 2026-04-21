package com.otpservice.service.notification;

import com.otpservice.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TelegramNotificationService implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String botToken;
    private final String apiUrl;
    private final String chatId;

    public TelegramNotificationService() {
        AppConfig cfg = AppConfig.getInstance();
        this.botToken = cfg.getTelegramBotToken();
        this.apiUrl = cfg.getTelegramApiUrl();
        this.chatId = cfg.getTelegramChatId();
    }

    @Override
    public void send(String destination, String code, String operationId) throws Exception {
        String text = "Your OTP for operation [" + operationId + "]: " + code;
        String url = apiUrl + botToken + "/sendMessage"
                   + "?chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8)
                   + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Telegram API error (status " + response.statusCode() + "): " + response.body());
        }
        log.info("OTP sent via TELEGRAM to chat_id={} for operation {}", chatId, operationId);
    }
}
