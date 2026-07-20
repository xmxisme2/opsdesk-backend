package com.opsdesk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** 自定义头像静态资源映射：头像仅含展示图片，不承载工单附件等业务文件。 */
@Configuration
public class AvatarResourceConfig implements WebMvcConfigurer {
    private final String avatarRoot;

    public AvatarResourceConfig(@Value("${opsdesk.storage.avatar-root:storage/avatars}") String avatarRoot) {
        this.avatarRoot = avatarRoot;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 使用绝对 file URI，避免 Windows 相对目录被运行目录变化影响。
        String location = Path.of(avatarRoot).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/avatars/**").addResourceLocations(location);
    }
}
