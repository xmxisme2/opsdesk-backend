package com.opsdesk.knowledge.converter;

import com.opsdesk.knowledge.entity.KnowledgeCategory;
import com.opsdesk.knowledge.mapper.KnowledgeArticleRow;
import com.opsdesk.knowledge.vo.KnowledgeArticleVO;
import com.opsdesk.knowledge.vo.KnowledgeCategoryVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/** 知识库实体到接口视图的转换器。 */
@Component
public class KnowledgeConverter {
    public KnowledgeArticleVO toArticleVO(KnowledgeArticleRow row) {
        KnowledgeArticleVO vo = new KnowledgeArticleVO();
        vo.setId(String.valueOf(row.getId()));
        vo.setTitle(row.getTitle());
        vo.setSummary(row.getSummary());
        vo.setContent(row.getContent());
        vo.setCategoryId(row.getCategoryId() == null ? null : String.valueOf(row.getCategoryId()));
        vo.setCategoryName(row.getCategoryName());
        vo.setTags(StringUtils.hasText(row.getTagNames()) ? Arrays.stream(row.getTagNames().split(",")).toList() : List.of());
        vo.setAttachments(List.of());
        vo.setSourceTicketId(row.getSourceTicketId() == null ? null : String.valueOf(row.getSourceTicketId()));
        vo.setSourceTicketNo(row.getSourceTicketNo());
        vo.setStatus(row.getStatus());
        vo.setAuthorId(String.valueOf(row.getAuthorId()));
        vo.setAuthorName(row.getAuthorName());
        vo.setViewCount(row.getViewCount() == null ? 0 : row.getViewCount());
        vo.setPublishedAt(row.getPublishedTime());
        vo.setCreatedAt(row.getCreateTime());
        vo.setUpdatedAt(row.getUpdateTime());
        return vo;
    }

    public KnowledgeCategoryVO toCategoryVO(KnowledgeCategory category) {
        KnowledgeCategoryVO vo = new KnowledgeCategoryVO();
        vo.setId(String.valueOf(category.getId()));
        vo.setParentId(category.getParentId() == null ? null : String.valueOf(category.getParentId()));
        vo.setName(category.getName());
        vo.setSort(category.getSort() == null ? 0 : category.getSort());
        vo.setEnabled(Integer.valueOf(1).equals(category.getEnabled()));
        return vo;
    }
}
