package com.example.rbac.module.auth.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Stateless JWT logout is implemented with an in-memory blacklist.
 * For a multi-instance deployment, replace this with Redis.
 */
@Component
public class JwtTokenBlacklist {

    private final Cache<String, Boolean> blacklist = CacheBuilder.newBuilder()
            .expireAfterWrite(7, TimeUnit.DAYS)
            .maximumSize(100_000)
            .build();

    public void invalidate(String token) {
        blacklist.put(token, true);
    }

    public boolean isInvalid(String token) {
        return blacklist.getIfPresent(token) != null;
    }
}
