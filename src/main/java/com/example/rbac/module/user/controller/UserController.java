package com.example.rbac.module.user.controller;

import com.example.rbac.common.api.ApiResponse;
import com.example.rbac.common.api.PageResponse;
import com.example.rbac.common.exception.ForbiddenException;
import com.example.rbac.common.security.RequirePermission;
import com.example.rbac.common.security.UserContext;
import com.example.rbac.module.role.entity.Role;
import com.example.rbac.module.user.dto.AssignRolesRequest;
import com.example.rbac.module.user.dto.CreateUserRequest;
import com.example.rbac.module.user.dto.FirstLoginPasswordRequest;
import com.example.rbac.module.user.dto.UpdatePasswordRequest;
import com.example.rbac.module.user.dto.UpdateUserRequest;
import com.example.rbac.module.user.entity.User;
import com.example.rbac.module.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @RequirePermission("user:create")
    @PostMapping
    public ApiResponse<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        Integer targetOrgId = userService.resolveTargetOrgId(
                UserContext.getCurrentUserId(), UserContext.getCurrentOrgId(), request.getOrgId());
        return ApiResponse.success(userService.createUser(targetOrgId, request));
    }

    @RequirePermission("user:view")
    @GetMapping
    public ApiResponse<PageResponse<User>> getUsers(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "realname", required = false) String realname,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "orgId", required = false) Integer orgId) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), normalizeSize(size));
        Integer targetOrgId = userService.resolveTargetOrgId(
                UserContext.getCurrentUserId(), UserContext.getCurrentOrgId(), orgId);
        Page<User> userPage = userService.getUsers(targetOrgId, realname, status, pageRequest);
        return ApiResponse.success(PageResponse.of(userPage));
    }

    @RequirePermission("user:view")
    @GetMapping("/{id}")
    public ApiResponse<User> getUser(@PathVariable Integer id) {
        return ApiResponse.success(userService.getUserById(
                id, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId()));
    }

    @RequirePermission("user:update")
    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable Integer id,
                                        @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.updateUser(
                id, request, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId()));
    }

    @RequirePermission("user:update")
    @PutMapping("/{id}/status")
    public ApiResponse<User> updateStatus(@PathVariable Integer id,
                                          @RequestParam Integer status) {
        return ApiResponse.success(userService.updateUserStatus(
                id, status, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId()));
    }

    /**
     * Self-service password change. Requires the old password, so no admin permission is needed.
     */
    @PutMapping("/{id}/password")
    public ApiResponse<User> updatePassword(@PathVariable Integer id,
                                            @Valid @RequestBody UpdatePasswordRequest request) {
        requireSelf(id);
        return ApiResponse.success(
                userService.updateUserPassword(id, request.getOldPassword(), request.getPassword()));
    }

    @RequirePermission("user:reset-password")
    @PutMapping("/{id}/reset-password")
    public ApiResponse<User> resetPassword(@PathVariable Integer id) {
        return ApiResponse.success(userService.resetUserPassword(
                id, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId()));
    }

    /**
     * Used after the first login or after an administrator reset the password.
     */
    @PutMapping("/{id}/first-login-password")
    public ApiResponse<User> updateFirstLoginPassword(@PathVariable Integer id,
                                                      @Valid @RequestBody FirstLoginPasswordRequest request) {
        requireSelf(id);
        return ApiResponse.success(userService.updateFirstLoginPassword(id, request.getPassword()));
    }

    @RequirePermission("user:delete")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId());
        return ApiResponse.success("用户已删除");
    }

    @RequirePermission("user:assign-role")
    @PostMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Integer id,
                                         @Valid @RequestBody AssignRolesRequest request) {
        userService.assignRolesToUser(
                id, request.getRoleIds(), UserContext.getCurrentUserId(), UserContext.getCurrentOrgId());
        return ApiResponse.success("角色分配成功");
    }

    @RequirePermission("user:view")
    @GetMapping("/{id}/roles")
    public ApiResponse<List<Role>> getUserRoles(@PathVariable Integer id) {
        return ApiResponse.success(userService.getUserRoles(
                id, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId()));
    }

    private void requireSelf(Integer id) {
        if (!UserContext.getCurrentUserId().equals(id)) {
            throw new ForbiddenException("只能修改自己的密码");
        }
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
