package com.otpservice.service;

import com.otpservice.repository.OtpCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OtpExpiryScheduler {
    private static final Logger log = LoggerFactory.getLogger(OtpExpiryScheduler.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "otp-expiry-scheduler");
        t.setDaemon(true);
        return t;
    });
    private final OtpCodeRepository otpCodeRepository;

    public OtpExpiryScheduler(OtpCodeRepository otpCodeRepository) {
        this.otpCodeRepository = otpCodeRepository;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::expireCodes, 60, 60, TimeUnit.SECONDS);
        log.info("OTP expiry scheduler started (interval: 60s)");
    }

    private void expireCodes() {
        try {
            int count = otpCodeRepository.expireOverdueCodes();
            if (count > 0) {
                log.info("Expired {} OTP code(s)", count);
            }
        } catch (Exception e) {
            log.error("Error during OTP expiry job", e);
        }
    }

    public void stop() {
        scheduler.shutdown();
        log.info("OTP expiry scheduler stopped");
    }
}
