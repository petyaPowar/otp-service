package com.otpservice.model;

import java.time.OffsetDateTime;

public class OtpCode {
    private Long id;
    private String operationId;
    private String code;
    private String status;
    private Long userId;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;

    public OtpCode() {}

    public OtpCode(String operationId, String code, String status,
                   Long userId, OffsetDateTime createdAt, OffsetDateTime expiresAt) {
        this.operationId = operationId;
        this.code = code;
        this.status = status;
        this.userId = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
