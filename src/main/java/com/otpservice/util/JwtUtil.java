package com.otpservice.util;

import com.otpservice.config.AppConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {
    private static final SecretKey KEY;
    private static final int EXPIRY_SECONDS;

    static {
        String secret = AppConfig.getInstance().getJwtSecret();
        if (secret == null || secret.length() < 32) {
            throw new RuntimeException("JWT secret must be at least 32 characters. Set JWT_SECRET env var.");
        }
        KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        EXPIRY_SECONDS = AppConfig.getInstance().getJwtExpirySeconds();
    }

    public static String generateToken(long userId, String login, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRY_SECONDS * 1000L);
        return Jwts.builder()
                   .subject(String.valueOf(userId))
                   .claim("login", login)
                   .claim("role", role)
                   .issuedAt(now)
                   .expiration(expiry)
                   .signWith(KEY)
                   .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                   .verifyWith(KEY)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }
}
