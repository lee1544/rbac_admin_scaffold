package com.example.rbac.module.permission.aspect;

import com.example.rbac.common.exception.ForbiddenException;
import com.example.rbac.common.exception.UnauthorizedException;
import com.example.rbac.common.security.RequirePermission;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                  RequirePermission requirePermission) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new ForbiddenException("无法获取请求上下文");
        }

        HttpServletRequest request = attributes.getRequest();
        if (Boolean.TRUE.equals(request.getAttribute("isDevBypass"))) {
            return joinPoint.proceed();
        }

        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("用户未登录");
        }

        List<String> permissions = getUserPermissions(request);
        if (!permissions.contains(requirePermission.value())) {
            throw new ForbiddenException("没有权限访问，缺少权限: " + requirePermission.value());
        }

        return joinPoint.proceed();
    }

    @SuppressWarnings("unchecked")
    private List<String> getUserPermissions(HttpServletRequest request) {
        Object value = request.getAttribute("userPermissions");
        return value instanceof List ? (List<String>) value : Collections.emptyList();
    }
}
