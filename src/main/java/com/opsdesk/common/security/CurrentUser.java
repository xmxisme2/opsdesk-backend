package com.opsdesk.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 当前登录用户上下文。
 *
 * <p>JWT 过滤器认证成功后写入 Spring Security，业务层从这里读取用户 ID、角色和权限。</p>
 */
public class CurrentUser {

    private final Long userId;
    private final String phone;
    private final String username;
    private final List<String> roles;
    private final List<String> permissions;

    public CurrentUser(Long userId,
                       String phone,
                       String username,
                       List<String> roles,
                       List<String> permissions) {
        this.userId = userId;
        this.phone = phone;
        this.username = username;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }

    public List<GrantedAuthority> toAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return Collections.unmodifiableList(authorities);
    }

    public Long getUserId() {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
