package com.example.rbac.module.permission.service;

import com.example.rbac.common.constant.SystemConstants;
import com.example.rbac.common.event.RolePermissionsChangedEvent;
import com.example.rbac.common.exception.BusinessException;
import com.example.rbac.common.exception.NotFoundException;
import com.example.rbac.module.permission.dto.CreatePermissionRequest;
import com.example.rbac.module.permission.dto.UpdatePermissionRequest;
import com.example.rbac.module.permission.entity.Permission;
import com.example.rbac.module.permission.entity.RolePermission;
import com.example.rbac.module.permission.repository.PermissionRepository;
import com.example.rbac.module.permission.repository.RolePermissionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PermissionService(PermissionRepository permissionRepository,
                             RolePermissionRepository rolePermissionRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Permission> search(String code, String name, Integer status) {
        return permissionRepository.search(code, name, status);
    }

    public Permission getById(Integer id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("权限不存在"));
    }

    @Transactional
    public Permission create(CreatePermissionRequest request) {
        String code = request.getPermissionCode().trim();
        if (permissionRepository.existsByPermissionCode(code)) {
            throw new BusinessException("权限编码已存在");
        }
        Permission permission = new Permission();
        permission.setPermissionCode(code);
        permission.setPermissionName(request.getPermissionName().trim());
        permission.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        return permissionRepository.save(permission);
    }

    @Transactional
    public Permission update(Integer id, UpdatePermissionRequest request) {
        Permission permission = getById(id);
        boolean authChanged = false;
        if (request.getPermissionCode() != null) {
            String code = request.getPermissionCode().trim();
            if (!code.equals(permission.getPermissionCode())
                    && permissionRepository.existsByPermissionCode(code)) {
                throw new BusinessException("权限编码已存在");
            }
            if (!code.equals(permission.getPermissionCode())) {
                authChanged = true;
            }
            permission.setPermissionCode(code);
        }
        if (request.getPermissionName() != null) {
            permission.setPermissionName(request.getPermissionName().trim());
        }
        if (request.getStatus() != null) {
            if (!Objects.equals(permission.getStatus(), request.getStatus())) {
                authChanged = true;
            }
            permission.setStatus(request.getStatus());
        }
        Permission saved = permissionRepository.save(permission);
        if (authChanged) {
            evictPermissionCaches(id);
        }
        return saved;
    }

    @Transactional
    public Permission updateStatus(Integer id, Integer status) {
        if (status == null
                || (status != SystemConstants.STATUS_ENABLED && status != SystemConstants.STATUS_DISABLED)) {
            throw new BusinessException("状态只能是0或1");
        }
        Permission permission = getById(id);
        permission.setStatus(status);
        Permission saved = permissionRepository.save(permission);
        evictPermissionCaches(id);
        return saved;
    }

    @Transactional
    public void delete(Integer id) {
        Permission permission = getById(id);
        if (rolePermissionRepository.existsByPermissionId(id)) {
            throw new BusinessException("该权限已分配给角色，请先解除角色关联");
        }
        permissionRepository.delete(permission);
    }

    public List<Permission> getAllActivePermissions() {
        return permissionRepository.findByStatus(1);
    }

    public List<Permission> getPermissionsByRoleId(Integer roleId) {
        return permissionRepository.findByRoleIds(Collections.singletonList(roleId));
    }

    public List<String> getPermissionCodesByRoleIds(Collection<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionRepository.findByRoleIds(roleIds).stream()
                .map(Permission::getPermissionCode)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignPermissionsToRole(Integer roleId, List<Integer> permissionIds) {
        rolePermissionRepository.deleteByRoleId(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<Integer> distinctIds = permissionIds.stream().distinct().collect(Collectors.toList());
            List<Permission> permissions = permissionRepository.findAllById(distinctIds);
            if (permissions.size() != distinctIds.size()) {
                throw new BusinessException("存在无效的权限ID");
            }
            List<RolePermission> relations = permissions.stream()
                    .map(permission -> {
                        RolePermission relation = new RolePermission();
                        relation.setRoleId(roleId);
                        relation.setPermissionId(permission.getId());
                        return relation;
                    })
                    .collect(Collectors.toList());
            rolePermissionRepository.saveAll(relations);
        }
        eventPublisher.publishEvent(new RolePermissionsChangedEvent(roleId));
    }

    @Transactional
    public void assignPermissionsByCodesToRole(Integer roleId, String[] permissionCodes) {
        rolePermissionRepository.deleteByRoleId(roleId);
        if (permissionCodes != null && permissionCodes.length > 0) {
            HashSet<String> codes = new HashSet<>();
            Collections.addAll(codes, permissionCodes);
            List<RolePermission> relations = codes.stream()
                    .map(code -> permissionRepository.findByPermissionCode(code)
                            .orElseThrow(() -> new BusinessException("权限不存在: " + code)))
                    .map(permission -> {
                        RolePermission relation = new RolePermission();
                        relation.setRoleId(roleId);
                        relation.setPermissionId(permission.getId());
                        return relation;
                    })
                    .collect(Collectors.toList());
            rolePermissionRepository.saveAll(relations);
        }
        eventPublisher.publishEvent(new RolePermissionsChangedEvent(roleId));
    }

    @Transactional
    public void clearRolePermissions(Integer roleId) {
        // Publish before deleting the relation so listeners can still resolve role members.
        eventPublisher.publishEvent(new RolePermissionsChangedEvent(roleId));
        rolePermissionRepository.deleteByRoleId(roleId);
    }

    /**
     * A permission change affects every role that references it. Evict the cached
     * permission snapshot of each affected user.
     */
    private void evictPermissionCaches(Integer permissionId) {
        rolePermissionRepository.findByPermissionId(permissionId).stream()
                .map(RolePermission::getRoleId)
                .distinct()
                .forEach(roleId -> eventPublisher.publishEvent(new RolePermissionsChangedEvent(roleId)));
    }
}
