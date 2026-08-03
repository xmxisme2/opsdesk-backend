package com.opsdesk.ai.dto;

/** 单篇索引重建请求，原因仅用于后续审计扩展。 */
public record IndexReindexRequest(String reason) {
}
