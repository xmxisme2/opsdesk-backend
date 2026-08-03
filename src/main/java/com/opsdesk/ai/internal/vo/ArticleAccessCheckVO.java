package com.opsdesk.ai.internal.vo;

import java.util.List;

/** 文章访问确认结果。 */
public record ArticleAccessCheckVO(List<String> accessibleArticleIds, List<String> deniedArticleIds) {
}
