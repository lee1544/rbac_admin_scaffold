package com.example.rbac.common.security;

import com.example.rbac.common.event.UserPermissionsChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserPermissionsCacheEvictListener {

    private final UserPermissionCache userPermissionCache;

    public UserPermissionsCacheEvictListener(UserPermissionCache userPermissionCache) {
        this.userPermissionCache = userPermissionCache;
    }

    @EventListener
    public void onUserPermissionsChanged(UserPermissionsChangedEvent event) {
        userPermissionCache.evict(event.getUserId());
    }
}
