package com.example.rbac.module.auth.security;

import com.example.rbac.common.constant.SystemConstants;
import com.example.rbac.common.security.UserPermissionCache;
import com.example.rbac.module.organization.entity.Organization;
import com.example.rbac.module.organization.repository.OrganizationRepository;
import com.example.rbac.module.permission.service.PermissionService;
import com.example.rbac.module.role.service.UserRoleService;
import com.example.rbac.module.user.entity.User;
import com.example.rbac.module.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenBlacklist jwtTokenBlacklist;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRoleService userRoleService;
    private final PermissionService permissionService;
    private final UserPermissionCache userPermissionCache;

    @Value("${dev.bypass-token:}")
    private String bypassToken;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   JwtTokenBlacklist jwtTokenBlacklist,
                                   UserRepository userRepository,
                                   OrganizationRepository organizationRepository,
                                   UserRoleService userRoleService,
                                   PermissionService permissionService,
                                   UserPermissionCache userPermissionCache) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtTokenBlacklist = jwtTokenBlacklist;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.userRoleService = userRoleService;
        this.permissionService = permissionService;
        this.userPermissionCache = userPermissionCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && !jwtTokenBlacklist.isInvalid(token)) {
            if (StringUtils.hasText(bypassToken) && bypassToken.equals(token)) {
                authenticateDeveloper(request);
            } else if (jwtTokenProvider.validateToken(token)) {
                authenticateUser(request, token);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Development bypass: identity is resolved from the seeded admin account so that
     * organization-scoped code still works. Never enable dev.bypass-token in production.
     */
    private void authenticateDeveloper(HttpServletRequest request) {
        userRepository.findByUsername(SystemConstants.DEFAULT_ADMIN_USERNAME)
                .filter(this::isActiveUser)
                .ifPresent(user -> {
                    List<String> permissions = loadPermissions(user.getId());
                    request.setAttribute("userId", user.getId());
                    request.setAttribute("orgId", user.getOrgId());
                    request.setAttribute("username", user.getUsername());
                    request.setAttribute("isDevBypass", true);
                    request.setAttribute("userPermissions", permissions);
                    setAuthentication(user.getUsername(), permissions);
                });
    }

    private void authenticateUser(HttpServletRequest request, String token) {
        String username = jwtTokenProvider.getUsername(token);
        Integer userId = jwtTokenProvider.getUserId(token);

        Optional<User> userOptional = userRepository.findById(userId)
                .filter(user -> user.getUsername().equals(username))
                .filter(this::isActiveUser);
        if (!userOptional.isPresent()) {
            // Deleted or disabled account: leave the request unauthenticated (401).
            return;
        }
        User user = userOptional.get();

        List<String> permissions = loadPermissions(user.getId());

        request.setAttribute("userId", user.getId());
        request.setAttribute("orgId", user.getOrgId());
        request.setAttribute("username", user.getUsername());
        request.setAttribute("userPermissions", permissions);

        setAuthentication(user.getUsername(), permissions);
    }

    private void setAuthentication(String username, List<String> permissions) {
        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private List<String> loadPermissions(Integer userId) {
        List<String> permissions = userPermissionCache.get(userId);
        if (permissions == null) {
            List<Integer> roleIds = userRoleService.getRoleIdsByUserId(userId);
            permissions = permissionService.getPermissionCodesByRoleIds(roleIds);
            userPermissionCache.put(userId, permissions);
        }
        return permissions;
    }

    private boolean isActiveUser(User user) {
        if (!Integer.valueOf(SystemConstants.STATUS_ENABLED).equals(user.getStatus())) {
            return false;
        }
        return organizationRepository.findById(user.getOrgId())
                .map(Organization::getStatus)
                .map(status -> Integer.valueOf(SystemConstants.STATUS_ENABLED).equals(status))
                .orElse(false);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
