package com.example.rbac.module.role.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class AssignPermissionsRequest {

    @NotNull(message = "权限列表不能为空")
    private List<Integer> permissionIds;
}
