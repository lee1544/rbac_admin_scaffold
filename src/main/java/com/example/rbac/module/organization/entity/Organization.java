package com.example.rbac.module.organization.entity;

import com.example.rbac.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "t_organization")
public class Organization extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "org_code", nullable = false, unique = true, length = 50)
    private String orgCode;

    @Column(name = "org_name", nullable = false, unique = true, length = 100)
    private String orgName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "status")
    private Integer status;
}
