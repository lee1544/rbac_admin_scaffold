package com.example.rbac.module.role.listener;

import com.example.rbac.common.event.RolePermissionsChangedEvent;
import com.example.rbac.common.security.UserPermissionCache;
import com.example.rbac.module.role.repository.UserRoleRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RolePermissionsCacheEvictListener {

    private final UserRoleRepository userRoleRepository;
    private final UserPermissionCache userPermissionCache;

    public RolePermissionsCacheEvictListener(UserRoleRepository userRoleRepository,
                                              UserPermissionCache userPermissionCache) {
        this.userRoleRepository = userRoleRepository;
        this.userPermissionCache = userPermissionCache;
    }

    @EventListener
    public void onRolePermissionsChanged(RolePermissionsChangedEvent event) {
        userRoleRepository.findUserIdsByRoleId(event.getRoleId())
                .forEach(userPermissionCache::evict);
    }
}
