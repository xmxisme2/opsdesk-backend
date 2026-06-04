package com.opsdesk.role.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 角色表实体。
 *
 * <p>映射 sys_role，登录后用于返回用户角色和组装 Spring Security 角色权限。</p>
 */
@Getter
@Setter
public class Role {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer builtIn;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
