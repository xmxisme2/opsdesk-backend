package com.opsdesk.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 后台编辑用户请求。
 *
 * <p>不包含密码字段，管理员如需改密必须走重置密码接口，避免资料保存时误改登录凭证。</p>
 */
@Getter
@Setter
public class UserUpdateRequest {

    @Size(max = 64, message = "昵称不能超过64个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱不能超过128个字符")
    private String email;

    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    private String gender;
    private String avatarCode;
    private String departmentId;
    private String status;
}
