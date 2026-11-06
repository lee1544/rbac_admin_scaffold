package com.example.rbac.module.role.repository;

import com.example.rbac.module.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRoleCodeAndOrgId(String roleCode, Integer orgId);

    Optional<Role> findByRoleCodeAndOrgIdIsNull(String roleCode);

    List<Role> findByOrgId(Integer orgId);

    boolean existsByRoleCodeAndOrgId(String roleCode, Integer orgId);

    @Query("select r from Role r where r.orgId = :orgId " +
            "and (:roleName is null or :roleName = '' or r.roleName like concat('%', concat(:roleName, '%'))) " +
            "order by r.id")
    List<Role> searchByOrgId(@Param("orgId") Integer orgId, @Param("roleName") String roleName);
}
