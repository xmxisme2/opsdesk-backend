package com.opsdesk.common.health;

import com.opsdesk.common.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 健康检查接口。
 *
 * <p>用于验证后端服务启动状态，后续可扩展数据库、Redis 等依赖检查。</p>
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @PostMapping("/check")
    public ApiResponse<Map<String, String>> check() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "service", "opsdesk-backend",
                "time", OffsetDateTime.now().toString()
        ));
    }
}

