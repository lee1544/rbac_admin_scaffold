package com.example.rbac.common.event;

/**
 * Published when a single user's role set changes, so cached permissions can be evicted.
 */
public class UserPermissionsChangedEvent {

    private final Integer userId;

    public UserPermissionsChangedEvent(Integer userId) {
        this.userId = userId;
    }

    public Integer getUserId() {
        return userId;
    }
}
