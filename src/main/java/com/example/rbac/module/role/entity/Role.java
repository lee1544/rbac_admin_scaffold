package com.example.rbac.module.role.entity;

import com.example.rbac.common.entity.BaseEntity;
import com.example.rbac.module.permission.entity.Permission;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "t_role", uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_code_org", columnNames = {"role_code", "org_id"})
})
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "description", length = 500)
    private String description;

    /**
     * null means a global role (for example SUPER_ADMIN).
     */
    @Column(name = "org_id")
    private Integer orgId;

    @Column(name = "status")
    private Integer status;

    @Transient
    private List<Permission> permissions;
}
