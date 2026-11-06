package com.example.rbac.module.permission.repository;

import com.example.rbac.module.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    Optional<Permission> findByPermissionCode(String permissionCode);

    boolean existsByPermissionCode(String permissionCode);

    List<Permission> findByStatus(Integer status);

    @Query("select distinct p from Permission p, RolePermission rp " +
            "where p.id = rp.permissionId and rp.roleId in :roleIds and p.status = 1")
    List<Permission> findByRoleIds(@Param("roleIds") Collection<Integer> roleIds);

    @Query("select p from Permission p where " +
            "(:code is null or :code = '' or p.permissionCode like concat('%', concat(:code, '%'))) and " +
            "(:name is null or :name = '' or p.permissionName like concat('%', concat(:name, '%'))) and " +
            "(:status is null or p.status = :status) order by p.id")
    List<Permission> search(@Param("code") String code,
                            @Param("name") String name,
                            @Param("status") Integer status);
}
