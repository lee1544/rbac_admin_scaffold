package com.example.rbac.module.user.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Data
public class UpdateUserRequest {

    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String realName;

    @Min(value = 0, message = "状态只能是0或1")
    @Max(value = 1, message = "状态只能是0或1")
    private Integer status;
}
