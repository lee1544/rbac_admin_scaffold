package com.example.rbac.module.permission.repository;

import com.example.rbac.module.permission.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Integer> {

    List<RolePermission> findByRoleId(Integer roleId);

    void deleteByRoleId(Integer roleId);

    void deleteByPermissionId(Integer permissionId);

    boolean existsByRoleId(Integer roleId);

    boolean existsByPermissionId(Integer permissionId);
}
