package com.opsdesk.knowledge.dto;

import com.opsdesk.common.pagination.PageQuery;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

/** 知识库文章分页筛选请求。 */
@Getter
@Setter
public class KnowledgeArticleSearchRequest extends PageQuery {
    private String keyword;
    private String categoryId;
    private String tag;
    private String status;
    private String authorId;

    public String normalizedKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    public String normalizedTag() {
        return StringUtils.hasText(tag) ? tag.trim() : null;
    }
}
