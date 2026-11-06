package com.example.rbac.common.event;

/**
 * Published when a role's permission set changes, so every user holding that role can be evicted.
 */
public class RolePermissionsChangedEvent {

    private final Integer roleId;

    public RolePermissionsChangedEvent(Integer roleId) {
        this.roleId = roleId;
    }

    public Integer getRoleId() {
        return roleId;
    }
}
