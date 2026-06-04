package com.opsdesk.department.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 部门表实体。
 *
 * <p>映射 sys_department，注册时校验用户选择的主部门是否存在且启用。</p>
 */
@Getter
@Setter
public class Department {

    private Long id;
    private Long parentId;
    private String name;
    private Long leaderId;
    private Integer enabled;
}
