package com.opsdesk.permission.entity;

import lombok.Getter;
import lombok.Setter;

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
    private String path;
    private String method;
}
