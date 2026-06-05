package com.opsdesk.role.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色响应对象。
 *
 * <p>面向前端角色管理页返回基础角色信息、内置标记、启停状态和已绑定权限 ID。</p>
 */
@Getter
@Setter
public class RoleVO {

    private String id;
    private String code;
    private String name;
    private String description;
    private Boolean builtIn;
    private Boolean enabled;
    private List<String> permissionIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
