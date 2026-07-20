package com.opsdesk.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 当前登录用户可自行维护的资料请求，不允许修改手机号、部门、角色和账号状态。 */
@Getter
@Setter
public class UserProfileUpdateRequest {
    @Size(max = 64, message = "昵称不能超过64个字符") private String nickname;
    @Email(message = "邮箱格式不正确") @Size(max = 128, message = "邮箱不能超过128个字符") private String email;
    private String gender;
    private String avatarCode;
}
