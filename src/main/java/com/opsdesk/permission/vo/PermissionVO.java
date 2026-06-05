package com.opsdesk.permission.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限响应对象。
 *
 * <p>同时支持菜单、按钮和接口权限；树形接口通过 children 表示层级关系。</p>
 */
@Getter
@Setter
public class PermissionVO {

    private String id;
    private String code;
    private String name;
    private String type;
    private String parentId;
    private String path;
    private String method;
    private Integer sort;
    private Boolean enabled;
    private List<PermissionVO> children = new ArrayList<>();
}
