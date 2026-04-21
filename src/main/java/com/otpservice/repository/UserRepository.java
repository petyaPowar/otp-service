package com.otpservice.repository;

import com.otpservice.db.DatabaseManager;
import com.otpservice.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    public Optional<User> findByLogin(String login) throws SQLException {
        String sql = "SELECT id, login, password, role, created_at FROM users WHERE login = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
                return Optional.empty();
            }
        }
    }

    public Optional<User> findById(long id) throws SQLException {
        String sql = "SELECT id, login, password, role, created_at FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
                return Optional.empty();
            }
        }
    }

    public boolean adminExists() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public User save(User user) throws SQLException {
        String sql = "INSERT INTO users (login, password, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getLogin());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) user.setId(rs.getLong(1));
            }
            log.info("User saved: login={}, role={}", user.getLogin(), user.getRole());
            return user;
        }
    }

    public List<User> findAllNonAdmins() throws SQLException {
        String sql = "SELECT id, login, role, created_at FROM users WHERE role = 'USER' ORDER BY id";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<User> users = new ArrayList<>();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setLogin(rs.getString("login"));
                u.setRole(rs.getString("role"));
                u.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
                users.add(u);
            }
            return users;
        }
    }

    public boolean deleteById(long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ? AND role = 'USER'";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) log.info("User deleted: id={}", id);
            return rows > 0;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setLogin(rs.getString("login"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return u;
    }
}
