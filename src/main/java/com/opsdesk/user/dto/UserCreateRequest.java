package com.opsdesk.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 后台创建用户请求。
 *
 * <p>管理员创建账号时必须指定手机号、初始密码、部门和角色；用户名可选，未传时默认使用手机号。</p>
 */
@Getter
@Setter
public class UserCreateRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 64, message = "用户名不能超过64个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须在 8 到 64 位之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称不能超过64个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱不能超过128个字符")
    private String email;

    private String gender;
    private String avatarCode;

    @NotBlank(message = "部门不能为空")
    private String departmentId;

    private String status;

    @NotEmpty(message = "角色列表不能为空")
    private List<String> roleIds;
}
