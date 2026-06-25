package com.opsdesk.attachment.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * 附件 multipart 配置。
 *
 * <p>容器层限制单文件 20MB、请求 21MB，业务层仍会执行扩展名、MIME、内容和数量校验。</p>
 */
@Configuration
public class AttachmentMultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(20));
        factory.setMaxRequestSize(DataSize.ofMegabytes(21));
        return factory.createMultipartConfig();
    }
}
