package com.example.rbac.common.constant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Constants and default permission definitions shared by the scaffold.
 */
public final class SystemConstants {

    private SystemConstants() {
    }

    public static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";
    public static final String ORG_ADMIN_ROLE_CODE_PREFIX = "ORG_ADMIN_";
    public static final String ORG_USER_ROLE_CODE_PREFIX = "ORG_USER_";

    public static final String DEFAULT_ORG_CODE = "DEFAULT_ORG";
    public static final String DEFAULT_ORG_NAME = "默认组织";
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    public static final String DEFAULT_SYSTEM_PASSWORD = "12345678";

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    /**
     * All permissions shipped with the scaffold: code -> display name.
     * Add new permissions here and they will be created on the next startup.
     */
    public static final Map<String, String> BASE_PERMISSIONS;

    static {
        Map<String, String> permissions = new LinkedHashMap<>();
        permissions.put("user:view", "查看用户");
        permissions.put("user:create", "创建用户");
        permissions.put("user:update", "更新用户");
        permissions.put("user:delete", "删除用户");
        permissions.put("user:assign-role", "分配用户角色");
        permissions.put("user:reset-password", "重置用户密码");

        permissions.put("org:view", "查看组织");
        permissions.put("org:create", "创建组织");
        permissions.put("org:update", "更新组织");
        permissions.put("org:delete", "删除组织");

        permissions.put("role:view", "查看角色");
        permissions.put("role:create", "创建角色");
        permissions.put("role:update", "更新角色");
        permissions.put("role:delete", "删除角色");
        permissions.put("role:assign-permission", "分配角色权限");

        permissions.put("permission:view", "查看权限");
        permissions.put("permission:create", "创建权限");
        permissions.put("permission:update", "更新权限");
        permissions.put("permission:delete", "删除权限");

        BASE_PERMISSIONS = Collections.unmodifiableMap(permissions);
    }

    /**
     * Organization administrator: full user/role management inside its own organization.
     */
    public static final String[] ORG_ADMIN_PERMISSIONS = new String[]{
            "user:view", "user:create", "user:update", "user:delete", "user:assign-role", "user:reset-password",
            "org:view", "org:update",
            "role:view", "role:create", "role:update", "role:delete", "role:assign-permission",
            "permission:view"
    };

    /**
     * Normal organization user: can view its organization and manage its own profile/password.
     */
    public static final String[] ORG_USER_PERMISSIONS = new String[]{
            "org:view"
    };
}
