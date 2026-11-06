package com.example.rbac.module.auth.service;

import com.example.rbac.common.constant.SystemConstants;
import com.example.rbac.common.exception.NotFoundException;
import com.example.rbac.common.exception.UnauthorizedException;
import com.example.rbac.module.auth.dto.LoginRequest;
import com.example.rbac.module.auth.dto.LoginResponse;
import com.example.rbac.module.auth.dto.UserInfo;
import com.example.rbac.module.auth.security.JwtTokenProvider;
import com.example.rbac.module.organization.entity.Organization;
import com.example.rbac.module.organization.repository.OrganizationRepository;
import com.example.rbac.module.organization.service.OrganizationService;
import com.example.rbac.module.permission.service.PermissionService;
import com.example.rbac.module.role.service.UserRoleService;
import com.example.rbac.module.user.entity.User;
import com.example.rbac.module.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRoleService userRoleService;
    private final PermissionService permissionService;
    private final OrganizationService organizationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       UserRoleService userRoleService,
                       PermissionService permissionService,
                       OrganizationService organizationService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.userRoleService = userRoleService;
        this.permissionService = permissionService;
        this.organizationService = organizationService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));

        if (!Integer.valueOf(SystemConstants.STATUS_ENABLED).equals(user.getStatus())) {
            throw new UnauthorizedException("账号已被禁用");
        }
        Organization organization = organizationRepository.findById(user.getOrgId())
                .orElseThrow(() -> new UnauthorizedException("所属组织不存在"));
        if (!Integer.valueOf(SystemConstants.STATUS_ENABLED).equals(organization.getStatus())) {
            throw new UnauthorizedException("所属组织已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        boolean firstLogin = user.getLastLoginTime() == null;
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId(), user.getOrgId());
        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                buildUserInfo(user, firstLogin));
    }

    public UserInfo getUserInfo(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        return buildUserInfo(user, user.getLastLoginTime() == null);
    }

    private UserInfo buildUserInfo(User user, boolean firstLogin) {
        List<Integer> roleIds = userRoleService.getRoleIdsByUserId(user.getId());

        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setOrgId(user.getOrgId());
        userInfo.setOrgName(organizationService.getOrgNameById(user.getOrgId()));
        userInfo.setRoles(userRoleService.getRoleCodesByUserId(user.getId()));
        userInfo.setPermissions(permissionService.getPermissionCodesByRoleIds(roleIds));
        userInfo.setFirstLogin(firstLogin);
        userInfo.setCreateTime(user.getCreateTime());
        return userInfo;
    }
}
