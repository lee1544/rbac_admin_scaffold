package com.example.rbac.module.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:604800}")
    private Long expiration;

    public String generateToken(String username, Integer userId, Integer orgId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("orgId", orgId);
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expiration * 1000);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiresAt)
                .signWith(SignatureAlgorithm.HS512, secretBytes())
                .compact();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Integer getUserId(String token) {
        return numberClaim(token, "userId");
    }

    public Integer getOrgId(String token) {
        return numberClaim(token, "orgId");
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expiration;
    }

    private Integer numberClaim(String token, String name) {
        Object value = parseClaims(token).get(name);
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretBytes())
                .parseClaimsJws(token)
                .getBody();
    }

    private byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
