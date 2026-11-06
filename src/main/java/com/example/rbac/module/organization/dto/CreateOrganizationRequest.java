package com.example.rbac.module.organization.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateOrganizationRequest {

    @NotBlank(message = "组织编码不能为空")
    @Size(max = 50, message = "组织编码长度不能超过50个字符")
    private String orgCode;

    @NotBlank(message = "组织名称不能为空")
    @Size(max = 100, message = "组织名称长度不能超过100个字符")
    private String orgName;

    @Size(max = 500, message = "组织描述长度不能超过500个字符")
    private String description;

    @NotBlank(message = "管理员用户名不能为空")
    @Size(min = 3, max = 50, message = "管理员用户名长度3-50个字符")
    private String adminUsername;

    @NotBlank(message = "管理员密码不能为空")
    @Size(min = 6, max = 50, message = "管理员密码长度6-50个字符")
    private String adminPassword;

    @NotBlank(message = "管理员姓名不能为空")
    @Size(max = 50, message = "管理员姓名长度不能超过50个字符")
    private String adminRealName;
}
