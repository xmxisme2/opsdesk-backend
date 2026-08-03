package com.opsdesk.ai.vo;

import java.time.OffsetDateTime;

/**
 * 独立 AI 服务健康状态。
 *
 * @param status               总体状态
 * @param service              服务名
 * @param aiEnabled            AI 总开关最终状态
 * @param ragEnabled           RAG 开关最终状态
 * @param databaseStatus       AI 独立数据库状态
 * @param redisStatus          Redis 状态
 * @param serviceJwtConfigured Service JWT 是否已配置
 * @param checkedAt            检查时间
 */
public record AiServiceHealthVO(
        String status,
        String service,
        boolean aiEnabled,
        boolean ragEnabled,
        String databaseStatus,
        String redisStatus,
        boolean serviceJwtConfigured,
        OffsetDateTime checkedAt
) {
}
