package com.example.rbac.module.auth.controller;

import com.example.rbac.common.api.ApiResponse;
import com.example.rbac.common.security.UserContext;
import com.example.rbac.module.auth.dto.LoginRequest;
import com.example.rbac.module.auth.dto.LoginResponse;
import com.example.rbac.module.auth.dto.UserInfo;
import com.example.rbac.module.auth.security.JwtTokenBlacklist;
import com.example.rbac.module.auth.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenBlacklist jwtTokenBlacklist;

    public AuthController(AuthService authService, JwtTokenBlacklist jwtTokenBlacklist) {
        this.authService = authService;
        this.jwtTokenBlacklist = jwtTokenBlacklist;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/userinfo")
    public ApiResponse<UserInfo> getUserInfo() {
        return ApiResponse.success(authService.getUserInfo(UserContext.getCurrentUserId()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            jwtTokenBlacklist.invalidate(bearer.substring(7));
        }
        return ApiResponse.success("已退出登录");
    }
}
