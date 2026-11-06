package com.example.rbac.module.user.repository;

import com.example.rbac.module.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByOrgId(Integer orgId);

    long countByOrgId(Integer orgId);

    @Query("select u from User u where u.orgId = :orgId " +
            "and (:realName is null or :realName = '' or u.realName like concat('%', concat(:realName, '%'))) " +
            "and (:status is null or u.status = :status)")
    Page<User> search(@Param("orgId") Integer orgId,
                      @Param("realName") String realName,
                      @Param("status") Integer status,
                      Pageable pageable);
}
