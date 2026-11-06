package com.example.rbac.common.security;

import com.example.rbac.common.exception.UnauthorizedException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Convenience accessors for the authenticated principal of the current request.
 */
public final class UserContext {

    private UserContext() {
    }

    public static Integer getCurrentUserId() {
        HttpServletRequest request = currentRequest();
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("用户未登录");
        }
        return userId;
    }

    public static Integer getCurrentOrgId() {
        HttpServletRequest request = currentRequest();
        Integer orgId = (Integer) request.getAttribute("orgId");
        if (orgId == null) {
            throw new UnauthorizedException("用户未登录");
        }
        return orgId;
    }

    public static String getCurrentUsername() {
        HttpServletRequest request = currentRequest();
        String username = (String) request.getAttribute("username");
        if (username == null) {
            throw new UnauthorizedException("用户未登录");
        }
        return username;
    }

    public static boolean isDevBypass() {
        HttpServletRequest request = currentRequest();
        return Boolean.TRUE.equals(request.getAttribute("isDevBypass"));
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new UnauthorizedException("无法获取请求上下文");
        }
        return attributes.getRequest();
    }
}
