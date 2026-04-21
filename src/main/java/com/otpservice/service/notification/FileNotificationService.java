package com.otpservice.service.notification;

import com.otpservice.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileNotificationService implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(FileNotificationService.class);

    private final Path outputDir;

    public FileNotificationService() {
        String dir = AppConfig.getInstance().getOtpFileOutputDir();
        this.outputDir = Paths.get(dir);
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create OTP file output directory: " + dir, e);
        }
    }

    @Override
    public void send(String destination, String code, String operationId) throws IOException {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String filename = operationId + "_" + timestamp + ".txt";
        Path filePath = outputDir.resolve(filename);

        String content = "Operation: " + operationId + "\n"
                       + "Code:      " + code + "\n"
                       + "Generated: " + LocalDateTime.now() + "\n";

        Files.writeString(filePath, content, StandardOpenOption.CREATE_NEW);
        log.info("OTP written to file {} for operation {}", filePath.toAbsolutePath(), operationId);
    }
}
