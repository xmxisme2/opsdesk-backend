package com.opsdesk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置。
 *
 * <p>后续接口实现后，Swagger 页面用于校验接口契约和联调。</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI opsdeskOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OpsDesk API")
                        .version("v1")
                        .description("OpsDesk 智能工单协作平台后端接口文档"));
    }
}

