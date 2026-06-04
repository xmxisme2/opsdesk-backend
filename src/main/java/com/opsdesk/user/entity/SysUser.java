package com.opsdesk.user.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户表实体。
 *
 * <p>映射 sys_user，保存登录账号、密码哈希、资料、部门和账号状态。</p>
 */
@Getter
@Setter
public class SysUser {

    private Long id;
    private String phone;
    private String passwordHash;
    private String username;
    private String nickname;
    private String email;
    private String gender;
    private String avatarCode;
    private String avatarUrl;
    private Long departmentId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
