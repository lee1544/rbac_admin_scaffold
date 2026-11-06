package com.example.rbac.module.role.service;

import com.example.rbac.common.constant.SystemConstants;
import com.example.rbac.module.role.entity.Role;
import com.example.rbac.module.role.repository.RoleRepository;
import com.example.rbac.module.role.repository.UserRoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public UserRoleService(UserRoleRepository userRoleRepository, RoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    public List<Integer> getRoleIdsByUserId(Integer userId) {
        return userRoleRepository.findRoleIdsByUserId(userId);
    }

    public List<Role> getRolesByUserId(Integer userId) {
        return roleRepository.findAllById(getRoleIdsByUserId(userId));
    }

    public List<String> getRoleCodesByUserId(Integer userId) {
        return getRolesByUserId(userId).stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toList());
    }

    public boolean isSuperAdmin(Integer userId) {
        return getRoleCodesByUserId(userId).contains(SystemConstants.SUPER_ADMIN_ROLE_CODE);
    }

    public boolean isOrgAdmin(Integer userId, Integer orgId) {
        String orgAdminCode = SystemConstants.ORG_ADMIN_ROLE_CODE_PREFIX + orgId;
        return getRoleCodesByUserId(userId).contains(orgAdminCode);
    }

    public UserRoleInfo getUserRoleInfo(Integer userId, Integer orgId) {
        List<String> roleCodes = getRoleCodesByUserId(userId);
        boolean superAdmin = roleCodes.contains(SystemConstants.SUPER_ADMIN_ROLE_CODE);
        boolean orgAdmin = roleCodes.contains(SystemConstants.ORG_ADMIN_ROLE_CODE_PREFIX + orgId);
        return new UserRoleInfo(superAdmin, orgAdmin);
    }

    public static class UserRoleInfo {
        private final boolean superAdmin;
        private final boolean orgAdmin;

        public UserRoleInfo(boolean superAdmin, boolean orgAdmin) {
            this.superAdmin = superAdmin;
            this.orgAdmin = orgAdmin;
        }

        public boolean isSuperAdmin() {
            return superAdmin;
        }

        public boolean isOrgAdmin() {
            return orgAdmin;
        }
    }
}
