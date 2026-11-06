package com.example.rbac.module.role.controller;

import com.example.rbac.common.api.ApiResponse;
import com.example.rbac.common.security.RequirePermission;
import com.example.rbac.common.security.UserContext;
import com.example.rbac.module.permission.entity.Permission;
import com.example.rbac.module.role.dto.AssignPermissionsRequest;
import com.example.rbac.module.role.dto.CreateRoleRequest;
import com.example.rbac.module.role.dto.UpdateRoleRequest;
import com.example.rbac.module.role.entity.Role;
import com.example.rbac.module.role.service.RoleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @RequirePermission("role:view")
    @GetMapping
    public ApiResponse<List<Role>> getRoles(
            @RequestParam(name = "roleName", required = false) String roleName,
            @RequestParam(name = "orgId", required = false) Integer orgId) {
        Integer targetOrgId = roleService.resolveTargetOrgId(
                UserContext.getCurrentUserId(), UserContext.getCurrentOrgId(), orgId);
        return ApiResponse.success(roleService.getRolesByOrgId(targetOrgId, roleName));
    }

    @RequirePermission("role:view")
    @GetMapping("/{id}")
    public ApiResponse<Role> getRole(@PathVariable Integer id) {
        return ApiResponse.success(roleService.getRoleById(
                id, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId()));
    }

    @RequirePermission("role:create")
    @PostMapping
    public ApiResponse<Role> createRole(@Valid @RequestBody CreateRoleRequest request) {
        Integer targetOrgId = roleService.resolveTargetOrgId(
                UserContext.getCurrentUserId(), UserContext.getCurrentOrgId(), request.getOrgId());
        return ApiResponse.success(roleService.createRole(targetOrgId, request));
    }

    @RequirePermission("role:update")
    @PutMapping("/{id}")
    public ApiResponse<Role> updateRole(@PathVariable Integer id,
                                        @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.success(roleService.updateRole(
                id, request, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId()));
    }

    @RequirePermission("role:delete")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Integer id) {
        roleService.deleteRole(id, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId());
        return ApiResponse.success("角色已删除");
    }

    @RequirePermission("role:assign-permission")
    @PostMapping("/{id}/permissions")
    public ApiResponse<Void> assignPermissions(@PathVariable Integer id,
                                               @Valid @RequestBody AssignPermissionsRequest request) {
        roleService.assignPermissionsToRole(
                id, request.getPermissionIds(), UserContext.getCurrentUserId(), UserContext.getCurrentOrgId());
        return ApiResponse.success("权限分配成功");
    }

    @RequirePermission("role:view")
    @GetMapping("/{id}/permissions")
    public ApiResponse<List<Permission>> getRolePermissions(@PathVariable Integer id) {
        return ApiResponse.success(roleService.getRolePermissions(
                id, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId()));
    }
}
