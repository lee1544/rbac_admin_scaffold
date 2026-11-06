package com.example.rbac.module.organization.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Data
public class UpdateOrganizationRequest {

    @Size(max = 50, message = "组织编码长度不能超过50个字符")
    private String orgCode;

    @Size(max = 100, message = "组织名称长度不能超过100个字符")
    private String orgName;

    @Size(max = 500, message = "组织描述长度不能超过500个字符")
    private String description;

    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是0或1")
    private Integer status;
}
