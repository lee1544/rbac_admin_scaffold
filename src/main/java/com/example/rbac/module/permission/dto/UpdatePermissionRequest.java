package com.example.rbac.module.permission.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Data
public class UpdatePermissionRequest {

    @Size(max = 100, message = "权限编码长度不能超过100个字符")
    private String permissionCode;

    @Size(max = 100, message = "权限名称长度不能超过100个字符")
    private String permissionName;

    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是1或0")
    private Integer status;
}
