package com.otpservice.repository;

import com.otpservice.db.DatabaseManager;
import com.otpservice.model.OtpCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

public class OtpCodeRepository {
    private static final Logger log = LoggerFactory.getLogger(OtpCodeRepository.class);

    public OtpCode save(OtpCode otpCode) throws SQLException {
        String sql = "INSERT INTO otp_codes (operation_id, code, status, user_id, created_at, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, otpCode.getOperationId());
            ps.setString(2, otpCode.getCode());
            ps.setString(3, otpCode.getStatus());
            ps.setLong(4, otpCode.getUserId());
            ps.setObject(5, otpCode.getCreatedAt());
            ps.setObject(6, otpCode.getExpiresAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) otpCode.setId(rs.getLong(1));
            }
            return otpCode;
        }
    }

    public Optional<OtpCode> findActiveByOperationIdAndUserId(String operationId, long userId)
            throws SQLException {
        String sql = "SELECT id, operation_id, code, status, user_id, created_at, expires_at " +
                     "FROM otp_codes " +
                     "WHERE operation_id = ? AND user_id = ? AND status = 'ACTIVE' " +
                     "ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, operationId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapOtpCode(rs));
                return Optional.empty();
            }
        }
    }

    public void markUsed(long id) throws SQLException {
        updateStatus(id, "USED");
        log.info("OTP code marked USED: id={}", id);
    }

    public void markExpired(long id) throws SQLException {
        updateStatus(id, "EXPIRED");
    }

    public int expireOverdueCodes() throws SQLException {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' " +
                     "WHERE status = 'ACTIVE' AND expires_at < NOW()";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    private void updateStatus(long id, String status) throws SQLException {
        String sql = "UPDATE otp_codes SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private OtpCode mapOtpCode(ResultSet rs) throws SQLException {
        OtpCode c = new OtpCode();
        c.setId(rs.getLong("id"));
        c.setOperationId(rs.getString("operation_id"));
        c.setCode(rs.getString("code"));
        c.setStatus(rs.getString("status"));
        c.setUserId(rs.getLong("user_id"));
        c.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        c.setExpiresAt(rs.getObject("expires_at", java.time.OffsetDateTime.class));
        return c;
    }
}
