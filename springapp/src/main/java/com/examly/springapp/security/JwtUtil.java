package com.examly.springapp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms.student}")
    private long studentExpiry;

    @Value("${app.jwt.expiration-ms.parent}")
    private long parentExpiry;

    @Value("${app.jwt.expiration-ms.teacher}")
    private long teacherExpiry;

    @Value("${app.jwt.expiration-ms.admin}")
    private long adminExpiry;

    @Value("${app.jwt.expiration-ms.default}")
    private long defaultExpiry;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public long resolveExpiry(String role) {
        if (role == null) return defaultExpiry;
        return switch (role) {
            case "STUDENT" -> studentExpiry;
            case "PARENT" -> parentExpiry;
            case "TEACHER", "CLASS_TEACHER" -> teacherExpiry;
            case "ADMIN", "PRINCIPAL" -> adminExpiry;
            default -> defaultExpiry;
        };
    }

    public String generateToken(String username, String role, Long userId) {
        long expiry = resolveExpiry(role);
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiry);
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, c -> c.get("role", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenValid(String token, String username) {
        try {
            String tokenUsername = extractUsername(token);
            return tokenUsername.equals(username) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
