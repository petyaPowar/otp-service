package com.otpservice.repository;

import com.otpservice.db.DatabaseManager;
import com.otpservice.model.OtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class OtpConfigRepository {
    private static final Logger log = LoggerFactory.getLogger(OtpConfigRepository.class);

    public OtpConfig findConfig() throws SQLException {
        String sql = "SELECT code_length, ttl_seconds FROM otp_config WHERE id = 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new OtpConfig(rs.getInt("code_length"), rs.getInt("ttl_seconds"));
            }
            throw new RuntimeException("OTP config not found — schema not initialized");
        }
    }

    public void updateConfig(int codeLength, int ttlSeconds) throws SQLException {
        String sql = "UPDATE otp_config SET code_length = ?, ttl_seconds = ? WHERE id = 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, codeLength);
            ps.setInt(2, ttlSeconds);
            ps.executeUpdate();
            log.info("OTP config updated: codeLength={}, ttlSeconds={}", codeLength, ttlSeconds);
        }
    }
}
