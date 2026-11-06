package com.example.rbac.module.organization.repository;

import com.example.rbac.module.organization.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {

    Optional<Organization> findByOrgCode(String orgCode);

    boolean existsByOrgCode(String orgCode);

    boolean existsByOrgName(String orgName);

    @Query("select o from Organization o where " +
            "(:orgName is null or :orgName = '' or o.orgName like concat('%', concat(:orgName, '%'))) " +
            "order by o.id")
    Page<Organization> search(@Param("orgName") String orgName, Pageable pageable);
}
