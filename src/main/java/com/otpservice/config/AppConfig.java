package com.otpservice.config;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties appProps = new Properties();
    private final Properties emailProps = new Properties();
    private final Properties smsProps = new Properties();
    private final Properties telegramProps = new Properties();

    private AppConfig() {
        load(appProps, "application.properties");
        load(emailProps, "email.properties");
        load(smsProps, "sms.properties");
        load(telegramProps, "telegram.properties");
    }

    public static AppConfig getInstance() { return INSTANCE; }

    private void load(Properties p, String resource) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (is == null) throw new RuntimeException("Resource not found: " + resource);
            p.load(is);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + resource, e);
        }
    }

    private String getEnvOrProp(String envKey, Properties props, String propKey, String def) {
        String val = System.getenv(envKey);
        return (val != null && !val.isBlank()) ? val : props.getProperty(propKey, def);
    }

    public int getServerPort() {
        return Integer.parseInt(appProps.getProperty("server.port", "8080"));
    }

    public int getThreadPoolSize() {
        return Integer.parseInt(appProps.getProperty("server.thread_pool_size", "10"));
    }

    public String getDbUrl() {
        return getEnvOrProp("DB_URL", appProps, "db.url", "jdbc:postgresql://localhost:5432/otpdb");
    }

    public String getDbUsername() {
        return getEnvOrProp("DB_USERNAME", appProps, "db.username", "postgres");
    }

    public String getDbPassword() {
        return getEnvOrProp("DB_PASSWORD", appProps, "db.password", "postgres");
    }

    public String getJwtSecret() {
        return getEnvOrProp("JWT_SECRET", appProps, "jwt.secret", "");
    }

    public int getJwtExpirySeconds() {
        return Integer.parseInt(appProps.getProperty("jwt.expiry_seconds", "3600"));
    }

    public String getOtpFileOutputDir() {
        return appProps.getProperty("otp.file_output_dir", "otp_codes");
    }

    public Properties getEmailProps() {
        Properties merged = new Properties();
        merged.putAll(emailProps);
        merged.setProperty("mail.smtp.host",
                getEnvOrProp("SMTP_HOST", emailProps, "mail.smtp.host", "localhost"));
        merged.setProperty("mail.smtp.port",
                getEnvOrProp("SMTP_PORT", emailProps, "mail.smtp.port", "1025"));
        String smtpUser = getEnvOrProp("SMTP_USERNAME", emailProps, "mail.smtp.username", "");
        String smtpPass = getEnvOrProp("SMTP_PASSWORD", emailProps, "mail.smtp.password", "");
        if (!smtpUser.isBlank()) merged.setProperty("mail.smtp.username", smtpUser);
        if (!smtpPass.isBlank()) merged.setProperty("mail.smtp.password", smtpPass);
        return merged;
    }

    public Properties getSmsProps() {
        Properties merged = new Properties();
        merged.putAll(smsProps);
        merged.setProperty("smpp.host",
                getEnvOrProp("SMPP_HOST", smsProps, "smpp.host", "localhost"));
        merged.setProperty("smpp.port",
                getEnvOrProp("SMPP_PORT", smsProps, "smpp.port", "2775"));
        merged.setProperty("smpp.system_id",
                getEnvOrProp("SMPP_SYSTEM_ID", smsProps, "smpp.system_id", "smppclient1"));
        merged.setProperty("smpp.password",
                getEnvOrProp("SMPP_PASSWORD", smsProps, "smpp.password", "password"));
        return merged;
    }

    public String getTelegramBotToken() {
        return getEnvOrProp("TELEGRAM_BOT_TOKEN", telegramProps, "telegram.bot_token", "");
    }

    public String getTelegramChatId() {
        return getEnvOrProp("TELEGRAM_CHAT_ID", telegramProps, "telegram.chat_id", "");
    }

    public String getTelegramApiUrl() {
        return telegramProps.getProperty("telegram.api_url", "https://api.telegram.org/bot");
    }
}
