package com.otpservice.service.notification;

public interface NotificationChannel {
    void send(String destination, String code, String operationId) throws Exception;
}
