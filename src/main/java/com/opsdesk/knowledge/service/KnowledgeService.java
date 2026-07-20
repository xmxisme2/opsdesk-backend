package com.opsdesk.knowledge.service;

import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.knowledge.dto.*;
import com.opsdesk.knowledge.vo.KnowledgeArticleVO;
import com.opsdesk.knowledge.vo.KnowledgeCategoryVO;
import com.opsdesk.knowledge.vo.KnowledgeTagVO;

import java.util.List;

/** 知识库业务服务，统一处理文章权限、状态、分类标签和审计。 */
public interface KnowledgeService {
    PageResult<KnowledgeArticleVO> search(KnowledgeArticleSearchRequest request, CurrentUser user);
    KnowledgeArticleVO detail(String id, CurrentUser user);
    KnowledgeArticleVO create(KnowledgeArticleMutationRequest request, CurrentUser user, String ip, String userAgent);
    KnowledgeArticleVO update(String id, KnowledgeArticleMutationRequest request, CurrentUser user, String ip, String userAgent);
    void delete(String id, KnowledgeActionRequest request, CurrentUser user, String ip, String userAgent);
    KnowledgeArticleVO fromTicket(String ticketId, KnowledgeFromTicketRequest request, CurrentUser user, String ip, String userAgent);
    KnowledgeArticleVO publish(String id, KnowledgeActionRequest request, CurrentUser user, String ip, String userAgent);
    KnowledgeArticleVO offline(String id, KnowledgeActionRequest request, CurrentUser user, String ip, String userAgent);
    List<KnowledgeCategoryVO> categoryTree(KnowledgeCategoryTreeRequest request);
    KnowledgeCategoryVO createCategory(KnowledgeCategoryMutationRequest request, CurrentUser user, String ip, String userAgent);
    KnowledgeCategoryVO updateCategory(String id, KnowledgeCategoryMutationRequest request, CurrentUser user, String ip, String userAgent);
    void deleteCategory(String id, CurrentUser user, String ip, String userAgent);
    List<KnowledgeTagVO> searchTags(KnowledgeTagSearchRequest request);
    KnowledgeTagVO createTag(KnowledgeTagCreateRequest request, CurrentUser user);
    void deleteTag(String id, CurrentUser user);
}
