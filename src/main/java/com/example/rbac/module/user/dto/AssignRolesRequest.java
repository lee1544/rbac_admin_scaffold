package com.example.rbac.module.user.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class AssignRolesRequest {

    @NotNull(message = "角色列表不能为空")
    private List<Integer> roleIds;
}
