package com.example.rbac.module.user.service;

import com.example.rbac.common.constant.SystemConstants;
import com.example.rbac.common.event.UserPermissionsChangedEvent;
import com.example.rbac.common.exception.BusinessException;
import com.example.rbac.common.exception.ForbiddenException;
import com.example.rbac.common.exception.NotFoundException;
import com.example.rbac.module.organization.repository.OrganizationRepository;
import com.example.rbac.module.role.entity.Role;
import com.example.rbac.module.role.entity.UserRole;
import com.example.rbac.module.role.repository.RoleRepository;
import com.example.rbac.module.role.repository.UserRoleRepository;
import com.example.rbac.module.role.service.UserRoleService;
import com.example.rbac.module.user.dto.CreateUserRequest;
import com.example.rbac.module.user.dto.UpdateUserRequest;
import com.example.rbac.module.user.entity.User;
import com.example.rbac.module.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       UserRoleService userRoleService,
                       PasswordEncoder passwordEncoder,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRoleService = userRoleService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public User createUser(Integer orgId, CreateUserRequest request) {
        return createUser(orgId, request.getUsername(), request.getPassword(),
                request.getRealName(), request.getRoleIds());
    }

    /**
     * Low-level creation used by both the user API and organization provisioning.
     */
    @Transactional
    public User createUser(Integer orgId,
                           String username,
                           String rawPassword,
                           String realName,
                           List<Integer> roleIds) {
        if (orgId == null) {
            throw new BusinessException("组织不能为空");
        }
        String normalizedUsername = username.trim();
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setOrgId(orgId);
        user.setStatus(SystemConstants.STATUS_ENABLED);
        userRepository.save(user);

        createUserRoles(user.getId(), orgId, resolveRoleIds(orgId, roleIds));
        return user;
    }

    /**
     * Super administrators may inspect another organization by passing orgId.
     */
    public Integer resolveTargetOrgId(Integer currentUserId, Integer currentOrgId, Integer requestedOrgId) {
        if (requestedOrgId == null || requestedOrgId.equals(currentOrgId)) {
            return currentOrgId;
        }
        if (!userRoleService.isSuperAdmin(currentUserId)) {
            throw new ForbiddenException("无权查看其他组织的用户");
        }
        return requestedOrgId;
    }

    public Page<User> getUsers(Integer orgId, String realName, Integer status, Pageable pageable) {
        Page<User> page = userRepository.search(orgId, realName, status, pageable);
        String orgName = getOrgName(orgId);
        page.forEach(user -> user.setOrgName(orgName));
        return page;
    }

    public User getUserById(Integer userId, Integer currentUserId, Integer currentOrgId) {
        User user = findUser(userId);
        checkOrgAccess(user, currentUserId, currentOrgId);
        user.setOrgName(getOrgName(user.getOrgId()));
        return user;
    }

    @Transactional
    public User updateUser(Integer userId,
                           UpdateUserRequest request,
                           Integer currentUserId,
                           Integer currentOrgId) {
        User user = findUser(userId);
        checkOrgAccess(user, currentUserId, currentOrgId);
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName().trim());
        }
        if (request.getStatus() != null) {
            validateStatus(request.getStatus());
            user.setStatus(request.getStatus());
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserStatus(Integer userId,
                                 Integer status,
                                 Integer currentUserId,
                                 Integer currentOrgId) {
        validateStatus(status);
        User user = findUser(userId);
        checkOrgAccess(user, currentUserId, currentOrgId);
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserPassword(Integer userId, String oldPassword, String newPassword) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public User updateFirstLoginPassword(Integer userId, String newPassword) {
        User user = findUser(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public User resetUserPassword(Integer userId, Integer currentUserId, Integer currentOrgId) {
        User user = findUser(userId);
        checkOrgAccess(user, currentUserId, currentOrgId);
        user.setPassword(passwordEncoder.encode(SystemConstants.DEFAULT_SYSTEM_PASSWORD));
        // Clearing lastLoginTime makes the next login report firstLogin=true.
        user.setLastLoginTime(null);
        userRepository.save(user);
        eventPublisher.publishEvent(new UserPermissionsChangedEvent(userId));
        return user;
    }

    @Transactional
    public void deleteUser(Integer userId, Integer currentUserId, Integer currentOrgId) {
        User user = findUser(userId);
        checkOrgAccess(user, currentUserId, currentOrgId);
        if (userId.equals(currentUserId)) {
            throw new BusinessException("不能删除当前登录用户");
        }
        if (userRoleService.isSuperAdmin(userId)) {
            throw new BusinessException("超级管理员不能删除");
        }
        userRoleRepository.deleteByUserId(userId);
        userRepository.delete(user);
        eventPublisher.publishEvent(new UserPermissionsChangedEvent(userId));
    }

    @Transactional
    public void assignRolesToUser(Integer userId,
                                  List<Integer> roleIds,
                                  Integer currentUserId,
                                  Integer currentOrgId) {
        User user = findUser(userId);
        boolean currentIsSuperAdmin = userRoleService.isSuperAdmin(currentUserId);
        if (!currentIsSuperAdmin && !Objects.equals(user.getOrgId(), currentOrgId)) {
            throw new ForbiddenException("无权操作其他组织的用户");
        }
        if (userRoleService.isSuperAdmin(userId) && !currentIsSuperAdmin) {
            throw new ForbiddenException("无权修改超级管理员的角色");
        }

        userRoleRepository.deleteByUserId(userId);
        createUserRoles(userId, user.getOrgId(), roleIds);
        eventPublisher.publishEvent(new UserPermissionsChangedEvent(userId));
    }

    public List<Role> getUserRoles(Integer userId, Integer currentUserId, Integer currentOrgId) {
        User user = findUser(userId);
        checkOrgAccess(user, currentUserId, currentOrgId);
        List<Integer> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleRepository.findAllById(roleIds);
    }

    private List<Integer> resolveRoleIds(Integer orgId, List<Integer> roleIds) {
        if (roleIds != null && !roleIds.isEmpty()) {
            return roleIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return roleRepository
                .findByRoleCodeAndOrgId(SystemConstants.ORG_USER_ROLE_CODE_PREFIX + orgId, orgId)
                .map(role -> Collections.singletonList(role.getId()))
                .orElse(Collections.emptyList());
    }

    private void createUserRoles(Integer userId, Integer orgId, List<Integer> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Integer roleId : roleIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList())) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new NotFoundException("角色不存在: " + roleId));
            if (!orgId.equals(role.getOrgId())) {
                throw new BusinessException("角色不属于该用户所在组织: " + role.getRoleName());
            }
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleRepository.save(userRole);
        }
    }

    private void checkOrgAccess(User user, Integer currentUserId, Integer currentOrgId) {
        if (userRoleService.isSuperAdmin(currentUserId)) {
            return;
        }
        if (!Objects.equals(user.getOrgId(), currentOrgId)) {
            throw new ForbiddenException("无权操作其他组织的用户");
        }
        if (userRoleService.isSuperAdmin(user.getId())) {
            throw new ForbiddenException("无权操作超级管理员");
        }
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
    }

    private String getOrgName(Integer orgId) {
        return organizationRepository.findById(orgId)
                .map(organization -> organization.getOrgName())
                .orElse("未知组织");
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != SystemConstants.STATUS_ENABLED && status != SystemConstants.STATUS_DISABLED)) {
            throw new BusinessException("状态只能是0或1");
        }
    }
}
