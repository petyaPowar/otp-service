package com.otpservice.service;

import com.otpservice.model.OtpCode;
import com.otpservice.model.OtpConfig;
import com.otpservice.repository.OtpCodeRepository;
import com.otpservice.repository.OtpConfigRepository;
import com.otpservice.service.notification.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;

public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final OtpCodeRepository otpCodeRepository;
    private final OtpConfigRepository otpConfigRepository;
    private final Map<String, NotificationChannel> channels;

    public OtpService(OtpCodeRepository otpCodeRepository,
                      OtpConfigRepository otpConfigRepository,
                      Map<String, NotificationChannel> channels) {
        this.otpCodeRepository = otpCodeRepository;
        this.otpConfigRepository = otpConfigRepository;
        this.channels = channels;
    }

    public void generateOtp(long userId, String operationId, String channel, String destination)
            throws SQLException, Exception {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }
        NotificationChannel notificationChannel = channels.get(channel.toUpperCase());
        if (notificationChannel == null) {
            throw new IllegalArgumentException("Unknown channel: " + channel + ". Valid: " + channels.keySet());
        }

        OtpConfig config = otpConfigRepository.findConfig();
        String code = generateCode(config.getCodeLength());
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusSeconds(config.getTtlSeconds());

        OtpCode otpCode = new OtpCode(operationId, code, "ACTIVE", userId, now, expiresAt);
        otpCodeRepository.save(otpCode);
        log.info("OTP generated for userId={}, operationId={}, channel={}", userId, operationId, channel);

        notificationChannel.send(destination, code, operationId);
    }

    public boolean validateOtp(long userId, String operationId, String code) throws SQLException {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }

        OtpCode otpCode = otpCodeRepository
                .findActiveByOperationIdAndUserId(operationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("No active OTP found for this operation"));

        if (otpCode.getExpiresAt().isBefore(OffsetDateTime.now())) {
            otpCodeRepository.markExpired(otpCode.getId());
            log.info("OTP validation failed (expired): operationId={}, userId={}", operationId, userId);
            return false;
        }

        if (!code.equals(otpCode.getCode())) {
            log.info("OTP validation failed (wrong code): operationId={}, userId={}", operationId, userId);
            return false;
        }

        otpCodeRepository.markUsed(otpCode.getId());
        log.info("OTP validated successfully: operationId={}, userId={}", operationId, userId);
        return true;
    }

    private String generateCode(int length) {
        int max = (int) Math.pow(10, length);
        int value = RNG.nextInt(max);
        return String.format("%0" + length + "d", value);
    }
}
