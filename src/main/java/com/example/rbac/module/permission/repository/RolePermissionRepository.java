package com.example.rbac.module.permission.repository;

import com.example.rbac.module.permission.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Integer> {

    List<RolePermission> findByRoleId(Integer roleId);

    List<RolePermission> findByPermissionId(Integer permissionId);

    /**
     * Bulk delete executed immediately. A derived delete would queue entity removals
     * until flush time, where Hibernate may insert the replacement rows first and
     * violate the unique constraint on (role_id, permission_id).
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from RolePermission rp where rp.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Integer roleId);

    @Modifying(flushAutomatically = true)
    @Query("delete from RolePermission rp where rp.permissionId = :permissionId")
    void deleteByPermissionId(@Param("permissionId") Integer permissionId);

    boolean existsByRoleId(Integer roleId);

    boolean existsByPermissionId(Integer permissionId);
}
