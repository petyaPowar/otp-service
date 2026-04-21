package com.otpservice.service.notification;

import com.otpservice.config.AppConfig;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class EmailNotificationService implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final Session session;
    private final String from;

    public EmailNotificationService() {
        Properties emailProps = AppConfig.getInstance().getEmailProps();
        this.from = emailProps.getProperty("mail.from", "noreply@otpservice.local");

        String username = emailProps.getProperty("mail.smtp.username", "");
        String password = emailProps.getProperty("mail.smtp.password", "");
        boolean auth = Boolean.parseBoolean(emailProps.getProperty("mail.smtp.auth", "false"));

        if (auth && !username.isBlank()) {
            this.session = Session.getInstance(emailProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            this.session = Session.getInstance(emailProps);
        }
    }

    @Override
    public void send(String destination, String code, String operationId) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destination));
        message.setSubject("Your OTP Code");
        message.setText("Your OTP code for operation [" + operationId + "]: " + code);
        Transport.send(message);
        log.info("OTP sent via EMAIL to {} for operation {}", destination, operationId);
    }
}
