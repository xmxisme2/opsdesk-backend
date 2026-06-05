package com.opsdesk.permission.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 权限表实体。
 *
 * <p>映射 sys_permission，首版用于返回用户权限编码，后续扩展菜单、按钮和接口权限树。</p>
 */
@Getter
@Setter
public class Permission {

    private Long id;
    private String code;
    private String name;
    private String type;
    private Long parentId;
    private String path;
    private String method;
    private Integer sort;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
