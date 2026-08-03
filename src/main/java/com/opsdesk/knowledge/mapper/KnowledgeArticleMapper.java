package com.opsdesk.knowledge.mapper;

import com.opsdesk.knowledge.entity.KnowledgeArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 知识库文章数据访问 Mapper，业务权限和状态规则由 Service 处理。 */
@Mapper
public interface KnowledgeArticleMapper {
    KnowledgeArticleRow findById(@Param("id") Long id);
    List<KnowledgeArticleRow> findPublishedAfterId(@Param("afterId") Long afterId, @Param("limit") int limit);
    List<KnowledgeArticleRow> search(@Param("keyword") String keyword,
                                     @Param("categoryId") Long categoryId,
                                     @Param("tag") String tag,
                                     @Param("status") String status,
                                     @Param("authorId") Long authorId,
                                     @Param("sortBy") String sortBy);
    int insert(KnowledgeArticle article);
    int update(KnowledgeArticle article);
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("published") boolean published, @Param("operatorId") Long operatorId);
    int logicalDelete(@Param("id") Long id, @Param("operatorId") Long operatorId);
    int incrementViewCount(@Param("id") Long id);
    int countByCategoryId(@Param("categoryId") Long categoryId);
    List<Long> findTagIds(@Param("articleId") Long articleId);
    int deleteArticleTags(@Param("articleId") Long articleId, @Param("operatorId") Long operatorId);
    int insertArticleTag(@Param("id") Long id, @Param("articleId") Long articleId,
                         @Param("tagId") Long tagId, @Param("operatorId") Long operatorId);
}
