package com.opsdesk.knowledge.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/** 知识库标签查询请求。 */
@Getter
@Setter
public class KnowledgeTagSearchRequest {
    private String keyword;
    private Integer limit = 50;

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    public int normalizedLimit() {
        return limit == null ? 50 : Math.max(1, Math.min(limit, 100));
    }
}
