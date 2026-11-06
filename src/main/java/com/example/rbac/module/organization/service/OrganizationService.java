package com.example.rbac.module.organization.service;

import com.example.rbac.common.constant.SystemConstants;
import com.example.rbac.common.event.UserPermissionsChangedEvent;
import com.example.rbac.common.exception.BusinessException;
import com.example.rbac.common.exception.ForbiddenException;
import com.example.rbac.common.exception.NotFoundException;
import com.example.rbac.module.organization.dto.CreateOrganizationRequest;
import com.example.rbac.module.organization.dto.UpdateOrganizationRequest;
import com.example.rbac.module.organization.entity.Organization;
import com.example.rbac.module.organization.repository.OrganizationRepository;
import com.example.rbac.module.permission.repository.RolePermissionRepository;
import com.example.rbac.module.role.entity.Role;
import com.example.rbac.module.role.repository.RoleRepository;
import com.example.rbac.module.role.repository.UserRoleRepository;
import com.example.rbac.module.role.service.RoleService;
import com.example.rbac.module.role.service.UserRoleService;
import com.example.rbac.module.user.entity.User;
import com.example.rbac.module.user.repository.UserRepository;
import com.example.rbac.module.user.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public OrganizationService(OrganizationRepository organizationRepository,
                               UserRepository userRepository,
                               RoleRepository roleRepository,
                               UserRoleRepository userRoleRepository,
                               RolePermissionRepository rolePermissionRepository,
                               RoleService roleService,
                               UserRoleService userRoleService,
                               UserService userService,
                               ApplicationEventPublisher eventPublisher) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleService = roleService;
        this.userRoleService = userRoleService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Organization createOrganization(CreateOrganizationRequest request) {
        String orgCode = request.getOrgCode().trim();
        String orgName = request.getOrgName().trim();
        if (organizationRepository.existsByOrgCode(orgCode)) {
            throw new BusinessException("组织编码已存在");
        }
        if (organizationRepository.existsByOrgName(orgName)) {
            throw new BusinessException("组织名称已存在");
        }

        Organization organization = new Organization();
        organization.setOrgCode(orgCode);
        organization.setOrgName(orgName);
        organization.setDescription(request.getDescription());
        organization.setStatus(SystemConstants.STATUS_ENABLED);
        organizationRepository.save(organization);

        Role adminRole = roleService.ensureSystemRole(
                organization.getId(),
                SystemConstants.ORG_ADMIN_ROLE_CODE_PREFIX + organization.getId(),
                "组织管理员",
                "管理本组织下的用户、角色和数据",
                SystemConstants.ORG_ADMIN_PERMISSIONS);

        roleService.ensureSystemRole(
                organization.getId(),
                SystemConstants.ORG_USER_ROLE_CODE_PREFIX + organization.getId(),
                "普通用户",
                "组织内的普通用户",
                SystemConstants.ORG_USER_PERMISSIONS);

        userService.createUser(
                organization.getId(),
                request.getAdminUsername(),
                request.getAdminPassword(),
                request.getAdminRealName(),
                Collections.singletonList(adminRole.getId()));

        return organization;
    }

    /**
     * Super administrators can list every organization; other users only see their own.
     */
    public Page<Organization> getOrganizations(String orgName,
                                               Pageable pageable,
                                               Integer currentUserId,
                                               Integer currentOrgId) {
        if (userRoleService.isSuperAdmin(currentUserId)) {
            return organizationRepository.search(orgName, pageable);
        }
        String keyword = orgName == null ? null : orgName.trim();
        return organizationRepository.findById(currentOrgId)
                .filter(org -> keyword == null || keyword.isEmpty() || org.getOrgName().contains(keyword))
                .map(org -> (Page<Organization>) new PageImpl<>(
                        Collections.singletonList(org), pageable, 1))
                .orElseGet(() -> Page.empty(pageable));
    }

    public Organization getOrganizationById(Integer id, Integer currentUserId, Integer currentOrgId) {
        Organization organization = findOrganization(id);
        checkOrgAccess(organization, currentUserId, currentOrgId);
        return organization;
    }

    public String getOrgNameById(Integer orgId) {
        return organizationRepository.findById(orgId)
                .map(Organization::getOrgName)
                .orElse("未知组织");
    }

    @Transactional
    public Organization updateOrganization(Integer id,
                                           UpdateOrganizationRequest request,
                                           Integer currentUserId,
                                           Integer currentOrgId) {
        Organization organization = findOrganization(id);
        checkOrgAccess(organization, currentUserId, currentOrgId);

        if (request.getOrgCode() != null) {
            String orgCode = request.getOrgCode().trim();
            if (!orgCode.equals(organization.getOrgCode())
                    && organizationRepository.existsByOrgCode(orgCode)) {
                throw new BusinessException("组织编码已存在");
            }
            organization.setOrgCode(orgCode);
        }

        if (request.getOrgName() != null) {
            String orgName = request.getOrgName().trim();
            if (!orgName.equals(organization.getOrgName())
                    && organizationRepository.existsByOrgName(orgName)) {
                throw new BusinessException("组织名称已存在");
            }
            organization.setOrgName(orgName);
        }

        if (request.getDescription() != null) {
            organization.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            organization.setStatus(request.getStatus());
        }
        return organizationRepository.save(organization);
    }

    /**
     * Deletes an organization together with its users, roles and relations.
     * The seeded DEFAULT_ORG is protected.
     */
    @Transactional
    public void deleteOrganization(Integer id, Integer currentUserId, Integer currentOrgId) {
        Organization organization = findOrganization(id);
        checkOrgAccess(organization, currentUserId, currentOrgId);
        if (SystemConstants.DEFAULT_ORG_CODE.equals(organization.getOrgCode())) {
            throw new BusinessException("默认组织不能删除");
        }

        List<User> users = userRepository.findByOrgId(id);
        for (User user : users) {
            eventPublisher.publishEvent(new UserPermissionsChangedEvent(user.getId()));
            userRoleRepository.deleteByUserId(user.getId());
        }
        userRepository.deleteAll(users);

        List<Role> roles = roleRepository.findByOrgId(id);
        for (Role role : roles) {
            rolePermissionRepository.deleteByRoleId(role.getId());
            userRoleRepository.deleteByRoleId(role.getId());
        }
        roleRepository.deleteAll(roles);

        organizationRepository.delete(organization);
    }

    private void checkOrgAccess(Organization organization, Integer currentUserId, Integer currentOrgId) {
        if (userRoleService.isSuperAdmin(currentUserId)) {
            return;
        }
        if (!Objects.equals(organization.getId(), currentOrgId)) {
            throw new ForbiddenException("无权操作其他组织");
        }
    }

    private Organization findOrganization(Integer id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("组织不存在"));
    }
}
