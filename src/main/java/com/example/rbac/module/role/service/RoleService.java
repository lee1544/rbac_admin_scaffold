package com.example.rbac.module.role.service;

import com.example.rbac.common.constant.SystemConstants;
import com.example.rbac.common.exception.BusinessException;
import com.example.rbac.common.exception.ForbiddenException;
import com.example.rbac.common.exception.NotFoundException;
import com.example.rbac.module.permission.entity.Permission;
import com.example.rbac.module.permission.service.PermissionService;
import com.example.rbac.module.role.dto.CreateRoleRequest;
import com.example.rbac.module.role.dto.UpdateRoleRequest;
import com.example.rbac.module.role.entity.Role;
import com.example.rbac.module.role.repository.RoleRepository;
import com.example.rbac.module.role.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionService permissionService;
    private final UserRoleService userRoleService;

    public RoleService(RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       PermissionService permissionService,
                       UserRoleService userRoleService) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.permissionService = permissionService;
        this.userRoleService = userRoleService;
    }

    /**
     * Super administrators may inspect another organization's roles by passing orgId.
     */
    public Integer resolveTargetOrgId(Integer currentUserId, Integer currentOrgId, Integer requestedOrgId) {
        if (requestedOrgId == null || requestedOrgId.equals(currentOrgId)) {
            return currentOrgId;
        }
        if (!userRoleService.isSuperAdmin(currentUserId)) {
            throw new ForbiddenException("无权查看其他组织的角色");
        }
        return requestedOrgId;
    }

    public List<Role> getRolesByOrgId(Integer orgId, String roleName) {
        List<Role> roles = roleRepository.searchByOrgId(orgId, roleName);
        roles.forEach(role -> role.setPermissions(permissionService.getPermissionsByRoleId(role.getId())));
        return roles;
    }

    public Role getRoleById(Integer id, Integer currentUserId, Integer currentOrgId) {
        Role role = findRole(id);
        checkRoleAccess(role, currentUserId, currentOrgId);
        role.setPermissions(permissionService.getPermissionsByRoleId(id));
        return role;
    }

    @Transactional
    public Role createRole(Integer orgId, CreateRoleRequest request) {
        String roleCode = request.getRoleCode().trim();
        if (roleRepository.existsByRoleCodeAndOrgId(roleCode, orgId)) {
            throw new BusinessException("角色编码已存在");
        }
        Role role = new Role();
        role.setRoleCode(roleCode);
        role.setRoleName(request.getRoleName().trim());
        role.setDescription(request.getDescription());
        role.setOrgId(orgId);
        role.setStatus(request.getStatus() == null ? SystemConstants.STATUS_ENABLED : request.getStatus());
        return roleRepository.save(role);
    }

    /**
     * Creates an organization-scoped system role only when it does not exist yet.
     * Used by organization creation and data initialization.
     */
    @Transactional
    public Role ensureSystemRole(Integer orgId,
                                 String roleCode,
                                 String roleName,
                                 String description,
                                 String[] permissionCodes) {
        Optional<Role> existing = roleRepository.findByRoleCodeAndOrgId(roleCode, orgId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Role role = new Role();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setDescription(description);
        role.setOrgId(orgId);
        role.setStatus(SystemConstants.STATUS_ENABLED);
        roleRepository.save(role);
        permissionService.assignPermissionsByCodesToRole(role.getId(), permissionCodes);
        return role;
    }

    @Transactional
    public Role updateRole(Integer id,
                           UpdateRoleRequest request,
                           Integer currentUserId,
                           Integer currentOrgId) {
        Role role = findRole(id);
        checkRoleAccess(role, currentUserId, currentOrgId);
        role.setRoleName(request.getRoleName().trim());
        role.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Integer id, Integer currentUserId, Integer currentOrgId) {
        Role role = findRole(id);
        checkRoleAccess(role, currentUserId, currentOrgId);
        String roleCode = role.getRoleCode();
        if (SystemConstants.SUPER_ADMIN_ROLE_CODE.equals(roleCode)) {
            throw new BusinessException("超级管理员角色不能删除");
        }
        if (roleCode.startsWith(SystemConstants.ORG_ADMIN_ROLE_CODE_PREFIX)) {
            throw new BusinessException("组织管理员角色不能删除");
        }
        if (roleCode.startsWith(SystemConstants.ORG_USER_ROLE_CODE_PREFIX)) {
            throw new BusinessException("组织普通用户角色不能删除");
        }

        // clearRolePermissions publishes the cache eviction event before relations are removed.
        permissionService.clearRolePermissions(id);
        userRoleRepository.deleteByRoleId(id);
        roleRepository.delete(role);
    }

    @Transactional
    public void assignPermissionsToRole(Integer roleId,
                                        List<Integer> permissionIds,
                                        Integer currentUserId,
                                        Integer currentOrgId) {
        Role role = findRole(roleId);
        checkRoleAccess(role, currentUserId, currentOrgId);
        permissionService.assignPermissionsToRole(roleId, permissionIds);
    }

    public List<Permission> getRolePermissions(Integer roleId,
                                               Integer currentUserId,
                                               Integer currentOrgId) {
        Role role = findRole(roleId);
        checkRoleAccess(role, currentUserId, currentOrgId);
        return permissionService.getPermissionsByRoleId(roleId);
    }

    private void checkRoleAccess(Role role, Integer currentUserId, Integer currentOrgId) {
        if (userRoleService.isSuperAdmin(currentUserId)) {
            return;
        }
        if (role.getOrgId() == null || !Objects.equals(role.getOrgId(), currentOrgId)) {
            throw new ForbiddenException("无权操作该角色");
        }
        if (SystemConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
            throw new ForbiddenException("无权操作超级管理员角色");
        }
    }

    private Role findRole(Integer id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("角色不存在"));
    }
}
