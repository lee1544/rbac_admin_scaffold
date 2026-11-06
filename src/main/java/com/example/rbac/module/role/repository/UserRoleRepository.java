package com.example.rbac.module.role.repository;

import com.example.rbac.module.role.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

    List<UserRole> findByUserId(Integer userId);

    List<UserRole> findByRoleId(Integer roleId);

    /**
     * Bulk delete executed immediately, so a subsequent re-assignment cannot
     * insert the same (user_id, role_id) pair before the old row is removed.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from UserRole ur where ur.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);

    @Modifying(flushAutomatically = true)
    @Query("delete from UserRole ur where ur.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Integer roleId);

    boolean existsByUserIdAndRoleId(Integer userId, Integer roleId);

    @Query("select ur.roleId from UserRole ur where ur.userId = :userId")
    List<Integer> findRoleIdsByUserId(@Param("userId") Integer userId);

    @Query("select ur.userId from UserRole ur where ur.roleId = :roleId")
    List<Integer> findUserIdsByRoleId(@Param("roleId") Integer roleId);
}
