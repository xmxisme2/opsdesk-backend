package com.opsdesk.auth.dto;

/**
 * 图形验证码请求参数。
 *
 * <p>scene 用于区分登录、注册等场景，首版主要支持登录场景。</p>
 */
public class CaptchaRequest {

    private String scene = "login";
    private String captchaType = "IMAGE";

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        this.captchaType = captchaType;
    }
}
