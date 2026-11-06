package com.example.rbac.module.organization.controller;

import com.example.rbac.common.api.ApiResponse;
import com.example.rbac.common.api.PageResponse;
import com.example.rbac.common.security.RequirePermission;
import com.example.rbac.common.security.UserContext;
import com.example.rbac.module.organization.dto.CreateOrganizationRequest;
import com.example.rbac.module.organization.dto.UpdateOrganizationRequest;
import com.example.rbac.module.organization.entity.Organization;
import com.example.rbac.module.organization.service.OrganizationService;
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

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @RequirePermission("org:create")
    @PostMapping
    public ApiResponse<Organization> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {
        return ApiResponse.success(organizationService.createOrganization(request));
    }

    @RequirePermission("org:view")
    @GetMapping
    public ApiResponse<PageResponse<Organization>> getOrganizations(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "orgName", required = false) String orgName) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), normalizeSize(size));
        return ApiResponse.success(PageResponse.of(organizationService.getOrganizations(
                orgName, pageRequest, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId())));
    }

    @RequirePermission("org:view")
    @GetMapping("/{id}")
    public ApiResponse<Organization> getOrganization(@PathVariable Integer id) {
        return ApiResponse.success(organizationService.getOrganizationById(
                id, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId()));
    }

    @RequirePermission("org:update")
    @PutMapping("/{id}")
    public ApiResponse<Organization> updateOrganization(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        return ApiResponse.success(organizationService.updateOrganization(
                id, request, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId()));
    }

    @RequirePermission("org:delete")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteOrganization(@PathVariable Integer id) {
        organizationService.deleteOrganization(
                id, UserContext.getCurrentUserId(), UserContext.getCurrentOrgId());
        return ApiResponse.success("组织已删除");
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
