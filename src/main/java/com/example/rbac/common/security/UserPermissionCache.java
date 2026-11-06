package com.example.rbac.common.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * In-memory permission cache: userId -> permission codes.
 * Evicted through UserPermissionsChangedEvent / RolePermissionsChangedEvent.
 */
@Component
public class UserPermissionCache {

    private final Cache<Integer, List<String>> cache = CacheBuilder.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    public List<String> get(Integer userId) {
        return cache.getIfPresent(userId);
    }

    public void put(Integer userId, List<String> permissions) {
        cache.put(userId, permissions);
    }

    public void evict(Integer userId) {
        if (userId != null) {
            cache.invalidate(userId);
        }
    }

    public void evictAll() {
        cache.invalidateAll();
    }
}
