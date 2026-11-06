package com.example.rbac.module.permission.controller;

import com.example.rbac.common.api.ApiResponse;
import com.example.rbac.common.security.RequirePermission;
import com.example.rbac.module.permission.dto.CreatePermissionRequest;
import com.example.rbac.module.permission.dto.UpdatePermissionRequest;
import com.example.rbac.module.permission.entity.Permission;
import com.example.rbac.module.permission.service.PermissionService;
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
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @RequirePermission("permission:view")
    @GetMapping
    public ApiResponse<List<Permission>> getPermissions(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "status", required = false) Integer status) {
        return ApiResponse.success(permissionService.search(code, name, status));
    }

    @RequirePermission("permission:view")
    @GetMapping("/{id}")
    public ApiResponse<Permission> getPermission(@PathVariable Integer id) {
        return ApiResponse.success(permissionService.getById(id));
    }

    @RequirePermission("permission:create")
    @PostMapping
    public ApiResponse<Permission> createPermission(
            @Valid @RequestBody CreatePermissionRequest request) {
        return ApiResponse.success(permissionService.create(request));
    }

    @RequirePermission("permission:update")
    @PutMapping("/{id}")
    public ApiResponse<Permission> updatePermission(
            @PathVariable Integer id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        return ApiResponse.success(permissionService.update(id, request));
    }

    @RequirePermission("permission:delete")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePermission(@PathVariable Integer id) {
        permissionService.delete(id);
        return ApiResponse.success("权限已删除");
    }
}
