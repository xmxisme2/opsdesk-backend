package com.opsdesk.user.dto;

/**
 * 默认头像查询请求。
 *
 * <p>gender 可选，传 MALE/FEMALE 时只返回对应性别头像。</p>
 */
public class AvatarOptionsRequest {

    private String gender;

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
