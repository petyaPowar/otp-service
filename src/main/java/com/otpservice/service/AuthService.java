package com.otpservice.service;

import com.otpservice.model.User;
import com.otpservice.repository.UserRepository;
import com.otpservice.util.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Set;

public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "USER");

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String login, String password, String role) throws SQLException {
        if (login == null || login.isBlank()) throw new IllegalArgumentException("Login must not be blank");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password must not be blank");
        if (role == null || !VALID_ROLES.contains(role.toUpperCase())) {
            throw new IllegalArgumentException("Role must be ADMIN or USER");
        }
        role = role.toUpperCase();

        if ("ADMIN".equals(role) && userRepository.adminExists()) {
            throw new IllegalStateException("Admin already exists");
        }
        if (userRepository.findByLogin(login).isPresent()) {
            throw new IllegalStateException("Login already taken");
        }

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User(login, hash, role);
        userRepository.save(user);
        log.info("User registered: login={}, role={}", login, role);
        return user;
    }

    public String login(String login, String password) throws SQLException {
        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        log.info("User logged in: login={}, role={}", login, user.getRole());
        return JwtUtil.generateToken(user.getId(), user.getLogin(), user.getRole());
    }
}
