package com.example.rbac.bootstrap;

import com.example.rbac.common.constant.SystemConstants;
import com.example.rbac.module.organization.entity.Organization;
import com.example.rbac.module.organization.repository.OrganizationRepository;
import com.example.rbac.module.permission.entity.Permission;
import com.example.rbac.module.permission.repository.PermissionRepository;
import com.example.rbac.module.permission.service.PermissionService;
import com.example.rbac.module.role.entity.Role;
import com.example.rbac.module.role.entity.UserRole;
import com.example.rbac.module.role.repository.RoleRepository;
import com.example.rbac.module.role.repository.UserRoleRepository;
import com.example.rbac.module.role.service.RoleService;
import com.example.rbac.module.user.entity.User;
import com.example.rbac.module.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Idempotent seed data. Runs on every startup and only creates what is missing.
 * Default account: admin / admin123 (change it immediately in real environments).
 */
@Component
@Order(1)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PermissionRepository permissionRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final PermissionService permissionService;

    public DataInitializer(PermissionRepository permissionRepository,
                           OrganizationRepository organizationRepository,
                           RoleRepository roleRepository,
                           UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           PasswordEncoder passwordEncoder,
                           RoleService roleService,
                           PermissionService permissionService) {
        this.permissionRepository = permissionRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPermissions();
        Organization defaultOrg = ensureDefaultOrganization();
        Role superAdminRole = ensureSuperAdminRole();
        ensureDefaultOrgRoles(defaultOrg);
        boolean adminCreated = ensureAdminUser(defaultOrg, superAdminRole);
        if (adminCreated) {
            log.info("RBAC scaffold initial data is ready. Seeded default account: {} / {} - change it immediately.",
                    SystemConstants.DEFAULT_ADMIN_USERNAME, SystemConstants.DEFAULT_ADMIN_PASSWORD);
        } else {
            log.info("RBAC scaffold initial data is ready (default account already exists, credentials unchanged).");
        }
    }

    private void seedPermissions() {
        for (Map.Entry<String, String> entry : SystemConstants.BASE_PERMISSIONS.entrySet()) {
            permissionRepository.findByPermissionCode(entry.getKey()).orElseGet(() -> {
                Permission permission = new Permission();
                permission.setPermissionCode(entry.getKey());
                permission.setPermissionName(entry.getValue());
                permission.setStatus(SystemConstants.STATUS_ENABLED);
                return permissionRepository.save(permission);
            });
        }
    }

    private Organization ensureDefaultOrganization() {
        return organizationRepository.findByOrgCode(SystemConstants.DEFAULT_ORG_CODE)
                .orElseGet(() -> {
                    Organization organization = new Organization();
                    organization.setOrgCode(SystemConstants.DEFAULT_ORG_CODE);
                    organization.setOrgName(SystemConstants.DEFAULT_ORG_NAME);
                    organization.setDescription("系统默认组织");
                    organization.setStatus(SystemConstants.STATUS_ENABLED);
                    return organizationRepository.save(organization);
                });
    }

    private Role ensureSuperAdminRole() {
        Role role = roleRepository.findByRoleCodeAndOrgIdIsNull(SystemConstants.SUPER_ADMIN_ROLE_CODE)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleCode(SystemConstants.SUPER_ADMIN_ROLE_CODE);
                    newRole.setRoleName("超级管理员");
                    newRole.setDescription("系统全局超级管理员");
                    newRole.setOrgId(null);
                    newRole.setStatus(SystemConstants.STATUS_ENABLED);
                    return roleRepository.save(newRole);
                });

        // Keep the super administrator aligned with the permissions shipped by the scaffold.
        permissionService.assignPermissionsByCodesToRole(
                role.getId(),
                SystemConstants.BASE_PERMISSIONS.keySet().toArray(new String[0]));
        return role;
    }

    private void ensureDefaultOrgRoles(Organization organization) {
        roleService.ensureSystemRole(
                organization.getId(),
                SystemConstants.ORG_ADMIN_ROLE_CODE_PREFIX + organization.getId(),
                "组织管理员",
                "管理本组织下的用户、角色和数据",
                SystemConstants.ORG_ADMIN_PERMISSIONS);

        roleService.ensureSystemRole(
                organization.getId(),
                SystemConstants.ORG_USER_ROLE_CODE_PREFIX + organization.getId(),
                "普通用户",
                "组织内的普通用户",
                SystemConstants.ORG_USER_PERMISSIONS);
    }

    private boolean ensureAdminUser(Organization organization, Role superAdminRole) {
        boolean created = false;
        Optional<User> existing = userRepository.findByUsername(SystemConstants.DEFAULT_ADMIN_USERNAME);
        User admin;
        if (existing.isPresent()) {
            admin = existing.get();
        } else {
            User newUser = new User();
            newUser.setUsername(SystemConstants.DEFAULT_ADMIN_USERNAME);
            newUser.setPassword(passwordEncoder.encode(SystemConstants.DEFAULT_ADMIN_PASSWORD));
            newUser.setRealName("超级管理员");
            newUser.setOrgId(organization.getId());
            newUser.setStatus(SystemConstants.STATUS_ENABLED);
            admin = userRepository.save(newUser);
            created = true;
        }

        if (!userRoleRepository.existsByUserIdAndRoleId(admin.getId(), superAdminRole.getId())) {
            UserRole userRole = new UserRole();
            userRole.setUserId(admin.getId());
            userRole.setRoleId(superAdminRole.getId());
            userRoleRepository.save(userRole);
        }
        return created;
    }
}
