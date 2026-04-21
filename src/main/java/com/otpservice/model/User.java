package com.otpservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;

public class User {
    private Long id;
    private String login;
    @JsonIgnore
    private String password;
    private String role;
    private OffsetDateTime createdAt;

    public User() {}

    public User(String login, String password, String role) {
        this.login = login;
        this.password = password;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
