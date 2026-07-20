package com.opsdesk.knowledge.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.attachment.dto.AttachmentSearchRequest;
import com.opsdesk.attachment.service.AttachmentResourceAccessService;
import com.opsdesk.attachment.service.AttachmentService;
import com.opsdesk.comment.entity.TicketComment;
import com.opsdesk.comment.mapper.TicketCommentMapper;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.pagination.PageHelperPageResult;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.knowledge.converter.KnowledgeConverter;
import com.opsdesk.knowledge.dto.*;
import com.opsdesk.knowledge.entity.KnowledgeArticle;
import com.opsdesk.knowledge.entity.KnowledgeCategory;
import com.opsdesk.knowledge.entity.KnowledgeTag;
import com.opsdesk.knowledge.mapper.KnowledgeArticleMapper;
import com.opsdesk.knowledge.mapper.KnowledgeArticleRow;
import com.opsdesk.knowledge.mapper.KnowledgeCategoryMapper;
import com.opsdesk.knowledge.mapper.KnowledgeTagMapper;
import com.opsdesk.knowledge.service.KnowledgeService;
import com.opsdesk.knowledge.vo.KnowledgeArticleVO;
import com.opsdesk.knowledge.vo.KnowledgeCategoryVO;
import com.opsdesk.knowledge.vo.KnowledgeTagVO;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.mapper.TicketMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库服务实现。
 *
 * <p>公开读取仅允许已发布文章；维护动作根据作者和角色收敛，并在同一事务内维护标签计数。</p>
 */
@Service
public class KnowledgeServiceImpl implements KnowledgeService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final String ROLE_AGENT = "AGENT";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final Set<String> TERMINAL_TICKET_STATUSES = Set.of("COMPLETED", "CLOSED");
    private static final Set<String> ARTICLE_STATUSES = Set.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_OFFLINE);

    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeTagMapper tagMapper;
    private final TicketMapper ticketMapper;
    private final TicketCommentMapper commentMapper;
    private final KnowledgeConverter converter;
    private final SnowflakeIdGenerator idGenerator;
    private final AuditLogService auditLogService;
    private final AttachmentService attachmentService;

    public KnowledgeServiceImpl(KnowledgeArticleMapper articleMapper, KnowledgeCategoryMapper categoryMapper,
                                KnowledgeTagMapper tagMapper, TicketMapper ticketMapper,
                                TicketCommentMapper commentMapper, KnowledgeConverter converter,
                                SnowflakeIdGenerator idGenerator, AuditLogService auditLogService,
                                AttachmentService attachmentService) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.ticketMapper = ticketMapper;
        this.commentMapper = commentMapper;
        this.converter = converter;
        this.idGenerator = idGenerator;
        this.auditLogService = auditLogService;
        this.attachmentService = attachmentService;
    }

    @Override
    public PageResult<KnowledgeArticleVO> search(KnowledgeArticleSearchRequest request, CurrentUser user) {
        KnowledgeArticleSearchRequest safe = request == null ? new KnowledgeArticleSearchRequest() : request;
        boolean maintainer = isMaintainer(user);
        String status = maintainer ? normalizeStatus(safe.getStatus(), null) : STATUS_PUBLISHED;
        Long authorId = maintainer ? parseOptional(safe.getAuthorId(), "作者ID") : null;
        Long categoryId = parseOptional(safe.getCategoryId(), "知识分类ID");
        String sortBy = "viewCount".equals(safe.getSortBy()) || "publishedAt".equals(safe.getSortBy())
                ? safe.getSortBy() : "updatedAt";
        return PageHelperPageResult.selectPage(safe,
                () -> articleMapper.search(safe.normalizedKeyword(), categoryId, safe.normalizedTag(), status, authorId, sortBy),
                converter::toArticleVO);
    }

    @Override
    public KnowledgeArticleVO detail(String id, CurrentUser user) {
        KnowledgeArticleRow row = requireArticle(IdParser.parseRequired(id, "知识文章ID"));
        if (!STATUS_PUBLISHED.equals(row.getStatus()) && !canEdit(row, user)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看未发布文章");
        }
        if (STATUS_PUBLISHED.equals(row.getStatus())) {
            articleMapper.incrementViewCount(row.getId());
            row.setViewCount((row.getViewCount() == null ? 0 : row.getViewCount()) + 1);
        }
        return toArticleVO(row, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleVO create(KnowledgeArticleMutationRequest request, CurrentUser user, String ip, String userAgent) {
        requireMaintainer(user);
        Long operatorId = requireUserId(user);
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(idGenerator.nextId());
        article.setAuthorId(operatorId);
        article.setStatus(STATUS_DRAFT);
        article.setViewCount(0L);
        article.setCreateBy(operatorId);
        applyArticle(article, request, operatorId, false);
        articleMapper.insert(article);
        replaceTags(article.getId(), request.getTags(), operatorId);
        attachmentService.bindTemporaryAttachments(AttachmentResourceAccessService.BIZ_TYPE_KNOWLEDGE,
                article.getId(), request.getAttachmentIds(), user);
        audit(operatorId, "KNOWLEDGE_CREATE", article.getId(), "创建知识库文章：" + article.getTitle(), ip, userAgent);
        return toArticleVO(requireArticle(article.getId()), user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleVO update(String id, KnowledgeArticleMutationRequest request, CurrentUser user, String ip, String userAgent) {
        Long articleId = IdParser.parseRequired(id, "知识文章ID");
        KnowledgeArticleRow current = requireArticle(articleId);
        if (!canEdit(current, user)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能编辑本人文章或由负责人维护");
        }
        applyArticle(current, request, requireUserId(user), STATUS_PUBLISHED.equals(current.getStatus()));
        articleMapper.update(current);
        replaceTags(articleId, request.getTags(), requireUserId(user));
        attachmentService.bindTemporaryAttachments(AttachmentResourceAccessService.BIZ_TYPE_KNOWLEDGE,
                articleId, request.getAttachmentIds(), user);
        audit(requireUserId(user), "KNOWLEDGE_UPDATE", articleId, "编辑知识库文章：" + current.getTitle(), ip, userAgent);
        return toArticleVO(requireArticle(articleId), user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, KnowledgeActionRequest request, CurrentUser user, String ip, String userAgent) {
        requireManager(user);
        Long articleId = IdParser.parseRequired(id, "知识文章ID");
        KnowledgeArticleRow row = requireArticle(articleId);
        clearTags(articleId, requireUserId(user));
        attachmentService.logicalDeleteBoundAttachments(AttachmentResourceAccessService.BIZ_TYPE_KNOWLEDGE, articleId, user);
        articleMapper.logicalDelete(articleId, requireUserId(user));
        audit(requireUserId(user), "KNOWLEDGE_DELETE", articleId, "删除知识库文章：" + row.getTitle() + remark(request), ip, userAgent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleVO fromTicket(String ticketId, KnowledgeFromTicketRequest request, CurrentUser user, String ip, String userAgent) {
        requireMaintainer(user);
        Long sourceId = IdParser.parseRequired(ticketId, "工单ID");
        Ticket ticket = ticketMapper.findById(sourceId);
        if (ticket == null) throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        if (!TERMINAL_TICKET_STATUSES.contains(ticket.getStatus())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "仅已完成或已关闭工单可生成知识草稿");
        }
        KnowledgeArticleMutationRequest mutation = new KnowledgeArticleMutationRequest();
        mutation.setTitle(ticket.getTitle());
        mutation.setSummary("来源工单 " + (ticket.getTicketNo() == null ? sourceId : ticket.getTicketNo()));
        StringBuilder content = new StringBuilder("# 问题描述\n\n").append(ticket.getDescription());
        KnowledgeFromTicketRequest safe = request == null ? new KnowledgeFromTicketRequest() : request;
        if (!Boolean.FALSE.equals(safe.getIncludeComments())) {
            // 知识文章可能后续发布给全部用户，生成草稿时只沉淀公开评论，内部备注不得跨域复制。
            List<TicketComment> comments = commentMapper.searchByTicketId(sourceId, false);
            if (!comments.isEmpty()) {
                content.append("\n\n# 处理记录\n");
                comments.forEach(comment -> content.append("\n- ").append(comment.getContent()));
            }
        }
        mutation.setContent(content.toString());
        // 工单分类与知识分类属于不同领域，不能直接复用 ID；草稿由维护者选择知识分类。
        mutation.setCategoryId(null);
        mutation.setSourceTicketId(String.valueOf(sourceId));
        KnowledgeArticleVO result = create(mutation, user, ip, userAgent);
        if (!Boolean.FALSE.equals(safe.getIncludeAttachments())) {
            // 附件复制只建立知识文章侧独立元数据引用，来源工单的附件记录保持不变。
            attachmentService.copyTicketAttachmentsToKnowledge(sourceId, Long.valueOf(result.getId()), user);
        } else {
            // 即使不复制附件，也要校验当前用户对来源工单的读取范围，不能仅凭工单 ID 生成草稿。
            AttachmentSearchRequest attachmentSearch = new AttachmentSearchRequest();
            attachmentSearch.setBizType(AttachmentResourceAccessService.BIZ_TYPE_TICKET);
            attachmentSearch.setBizId(String.valueOf(sourceId));
            attachmentService.search(attachmentSearch, user);
        }
        audit(requireUserId(user), "KNOWLEDGE_FROM_TICKET", Long.valueOf(result.getId()), "从工单生成知识草稿", ip, userAgent);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleVO publish(String id, KnowledgeActionRequest request, CurrentUser user, String ip, String ua) { return changeStatus(id, STATUS_PUBLISHED, request, user, ip, ua); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleVO offline(String id, KnowledgeActionRequest request, CurrentUser user, String ip, String ua) { return changeStatus(id, STATUS_OFFLINE, request, user, ip, ua); }

    protected KnowledgeArticleVO changeStatus(String id, String status, KnowledgeActionRequest request, CurrentUser user, String ip, String ua) {
        requireManager(user);
        Long articleId = IdParser.parseRequired(id, "知识文章ID");
        KnowledgeArticleRow row = requireArticle(articleId);
        if (status.equals(row.getStatus())) return toArticleVO(row, user);
        articleMapper.updateStatus(articleId, status, STATUS_PUBLISHED.equals(status), requireUserId(user));
        audit(requireUserId(user), STATUS_PUBLISHED.equals(status) ? "KNOWLEDGE_PUBLISH" : "KNOWLEDGE_OFFLINE",
                articleId, (STATUS_PUBLISHED.equals(status) ? "发布" : "下线") + "知识库文章：" + row.getTitle() + remark(request), ip, ua);
        return toArticleVO(requireArticle(articleId), user);
    }

    @Override
    public List<KnowledgeCategoryVO> categoryTree(KnowledgeCategoryTreeRequest request) {
        Integer enabled = request == null || request.getEnabled() == null ? null : (request.getEnabled() ? 1 : 0);
        List<KnowledgeCategoryVO> items = categoryMapper.searchTree(enabled).stream().map(converter::toCategoryVO).toList();
        Map<String, KnowledgeCategoryVO> byId = items.stream().collect(Collectors.toMap(KnowledgeCategoryVO::getId, item -> item));
        List<KnowledgeCategoryVO> roots = new ArrayList<>();
        items.forEach(item -> {
            KnowledgeCategoryVO parent = item.getParentId() == null ? null : byId.get(item.getParentId());
            if (parent == null) roots.add(item); else parent.getChildren().add(item);
        });
        return roots;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public KnowledgeCategoryVO createCategory(KnowledgeCategoryMutationRequest request, CurrentUser user, String ip, String ua) {
        requireManager(user); Long operatorId = requireUserId(user); Long parentId = parseOptional(request.getParentId(), "父级分类ID");
        if (parentId != null) requireCategory(parentId);
        String name = normalizeName(request.getName()); ensureCategoryUnique(parentId, name, null);
        KnowledgeCategory category = new KnowledgeCategory(); category.setId(idGenerator.nextId()); category.setParentId(parentId);
        category.setName(name); category.setSort(request.getSort() == null ? 0 : request.getSort()); category.setEnabled(Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1);
        category.setCreateBy(operatorId); category.setUpdateBy(operatorId);
        try { categoryMapper.insert(category); } catch (DuplicateKeyException e) { throw new BusinessException(ErrorCode.STATE_CONFLICT, "同级知识分类名称已存在"); }
        audit(operatorId,"KNOWLEDGE_CATEGORY_CREATE",category.getId(),"创建知识分类："+name,ip,ua); return converter.toCategoryVO(category);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public KnowledgeCategoryVO updateCategory(String id, KnowledgeCategoryMutationRequest request, CurrentUser user, String ip, String ua) {
        requireManager(user); Long operatorId=requireUserId(user); Long categoryId=IdParser.parseRequired(id,"知识分类ID"); KnowledgeCategory category=requireCategory(categoryId);
        Long parentId=parseOptional(request.getParentId(),"父级分类ID"); validateCategoryParent(categoryId,parentId); String name=normalizeName(request.getName()); ensureCategoryUnique(parentId,name,categoryId);
        category.setParentId(parentId); category.setName(name); category.setSort(request.getSort()==null?0:request.getSort()); category.setEnabled(Boolean.FALSE.equals(request.getEnabled())?0:1); category.setUpdateBy(operatorId);
        categoryMapper.update(category); audit(operatorId,"KNOWLEDGE_CATEGORY_UPDATE",categoryId,"编辑知识分类："+name,ip,ua); return converter.toCategoryVO(category);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String id, CurrentUser user, String ip, String ua) {
        requireManager(user); Long categoryId=IdParser.parseRequired(id,"知识分类ID"); KnowledgeCategory category=requireCategory(categoryId);
        if(categoryMapper.countChildren(categoryId)>0||articleMapper.countByCategoryId(categoryId)>0) throw new BusinessException(ErrorCode.STATE_CONFLICT,"分类存在子分类或关联文章，不能删除");
        categoryMapper.logicalDelete(categoryId,requireUserId(user)); audit(requireUserId(user),"KNOWLEDGE_CATEGORY_DELETE",categoryId,"删除知识分类："+category.getName(),ip,ua);
    }

    @Override public List<KnowledgeTagVO> searchTags(KnowledgeTagSearchRequest request) { KnowledgeTagSearchRequest safe=request==null?new KnowledgeTagSearchRequest():request; return tagMapper.search(safe.normalizedKeyword(),safe.normalizedLimit()).stream().map(this::toTagVO).toList(); }
    @Override @Transactional(rollbackFor = Exception.class) public KnowledgeTagVO createTag(KnowledgeTagCreateRequest request, CurrentUser user) { requireMaintainer(user); return toTagVO(ensureTag(normalizeTag(request.getName()),requireUserId(user))); }
    @Override @Transactional(rollbackFor = Exception.class) public void deleteTag(String id, CurrentUser user) { requireManager(user); Long tagId=IdParser.parseRequired(id,"知识标签ID"); KnowledgeTag tag=tagMapper.findById(tagId); if(tag==null)throw new BusinessException(ErrorCode.NOT_FOUND,"知识标签不存在"); if(tag.getArticleCount()!=null&&tag.getArticleCount()>0)throw new BusinessException(ErrorCode.STATE_CONFLICT,"标签仍有关联文章，不能删除"); tagMapper.logicalDelete(tagId,requireUserId(user)); }

    private void applyArticle(KnowledgeArticle article, KnowledgeArticleMutationRequest request, Long operatorId, boolean keepPublished) {
        article.setTitle(request.getTitle().trim()); article.setSummary(StringUtils.hasText(request.getSummary())?request.getSummary().trim():null); article.setContent(request.getContent().trim());
        Long categoryId=parseOptional(request.getCategoryId(),"知识分类ID"); if(categoryId!=null)requireCategory(categoryId); article.setCategoryId(categoryId);
        article.setSourceTicketId(parseOptional(request.getSourceTicketId(),"来源工单ID"));
        if(keepPublished){article.setStatus(STATUS_PUBLISHED);} else if(!STATUS_DRAFT.equals(article.getStatus())){article.setStatus(normalizeStatus(request.getStatus(),STATUS_DRAFT));}
        if(STATUS_PUBLISHED.equals(article.getStatus())&&article.getPublishedTime()==null)article.setPublishedTime(LocalDateTime.now()); article.setUpdateBy(operatorId);
    }

    private void replaceTags(Long articleId,List<String> names,Long operatorId){clearTags(articleId,operatorId); if(names==null)return; LinkedHashSet<String> unique=names.stream().filter(StringUtils::hasText).map(this::normalizeTag).collect(Collectors.toCollection(LinkedHashSet::new)); if(unique.size()>10)throw new BusinessException(ErrorCode.PARAM_ERROR,"每篇文章最多10个标签"); for(String name:unique){KnowledgeTag tag=ensureTag(name,operatorId); articleMapper.insertArticleTag(idGenerator.nextId(),articleId,tag.getId(),operatorId); tagMapper.increaseCount(tag.getId());}}
    private void clearTags(Long articleId,Long operatorId){List<Long> ids=articleMapper.findTagIds(articleId); if(!ids.isEmpty()){articleMapper.deleteArticleTags(articleId,operatorId); ids.forEach(tagMapper::decreaseCount);}}
    private KnowledgeTag ensureTag(String name,Long operatorId){KnowledgeTag existing=tagMapper.findByName(name); if(existing!=null)return existing; KnowledgeTag tag=new KnowledgeTag(); tag.setId(idGenerator.nextId()); tag.setName(name); tag.setCreateBy(operatorId); tag.setUpdateBy(operatorId); try{tagMapper.insert(tag);return tag;}catch(DuplicateKeyException e){KnowledgeTag concurrent=tagMapper.findByName(name);if(concurrent!=null)return concurrent;throw e;}}
    private void validateCategoryParent(Long id,Long parentId){if(parentId==null)return;if(id.equals(parentId))throw new BusinessException(ErrorCode.STATE_CONFLICT,"分类不能选择自身为父级");KnowledgeCategory parent=requireCategory(parentId);Set<Long> seen=new HashSet<>();while(parent!=null&&parent.getParentId()!=null){if(!seen.add(parent.getId())||id.equals(parent.getParentId()))throw new BusinessException(ErrorCode.STATE_CONFLICT,"分类父子关系不能形成循环");parent=categoryMapper.findById(parent.getParentId());}}
    private void ensureCategoryUnique(Long parentId,String name,Long excludeId){if(categoryMapper.countByParentAndName(parentId,name,excludeId)>0)throw new BusinessException(ErrorCode.STATE_CONFLICT,"同级知识分类名称已存在");}
    private KnowledgeCategory requireCategory(Long id){KnowledgeCategory c=categoryMapper.findById(id);if(c==null)throw new BusinessException(ErrorCode.NOT_FOUND,"知识分类不存在");return c;}
    private KnowledgeArticleRow requireArticle(Long id){KnowledgeArticleRow row=articleMapper.findById(id);if(row==null)throw new BusinessException(ErrorCode.NOT_FOUND,"知识文章不存在");return row;}
    /** 文章详情在基础字段之外补齐附件，读取由附件服务二次校验，避免附件接口被跨资源绕过。 */
    private KnowledgeArticleVO toArticleVO(KnowledgeArticleRow row, CurrentUser user) {
        KnowledgeArticleVO vo = converter.toArticleVO(row);
        AttachmentSearchRequest request = new AttachmentSearchRequest();
        request.setBizType(AttachmentResourceAccessService.BIZ_TYPE_KNOWLEDGE);
        request.setBizId(String.valueOf(row.getId()));
        vo.setAttachments(attachmentService.search(request, user));
        return vo;
    }
    private boolean canEdit(KnowledgeArticle article,CurrentUser user){return article.getAuthorId().equals(requireUserId(user))||hasAnyRole(user,ROLE_MANAGER,ROLE_ADMIN);}
    private boolean isMaintainer(CurrentUser user){return hasAnyRole(user,ROLE_AGENT,ROLE_MANAGER,ROLE_ADMIN);}
    private void requireMaintainer(CurrentUser user){if(!isMaintainer(user))throw new BusinessException(ErrorCode.FORBIDDEN,"需要知识库维护权限");}
    private void requireManager(CurrentUser user){if(!hasAnyRole(user,ROLE_MANAGER,ROLE_ADMIN))throw new BusinessException(ErrorCode.FORBIDDEN,"需要负责人或管理员权限");}
    private boolean hasAnyRole(CurrentUser user,String...roles){return user!=null&&Arrays.stream(roles).anyMatch(user.getRoles()::contains);}
    private Long requireUserId(CurrentUser user){if(user==null||user.getUserId()==null)throw new BusinessException(ErrorCode.UNAUTHORIZED,"请先登录");return user.getUserId();}
    private Long parseOptional(String value,String field){return StringUtils.hasText(value)?IdParser.parseRequired(value,field):null;}
    private String normalizeStatus(String value,String fallback){if(!StringUtils.hasText(value))return fallback;String status=value.trim().toUpperCase(Locale.ROOT);if(!ARTICLE_STATUSES.contains(status))throw new BusinessException(ErrorCode.PARAM_ERROR,"文章状态不合法");return status;}
    private String normalizeName(String value){String name=value.trim();if(name.isEmpty())throw new BusinessException(ErrorCode.PARAM_ERROR,"分类名称不能为空");return name;}
    private String normalizeTag(String value){String name=value.trim();if(name.isEmpty()||name.length()>64)throw new BusinessException(ErrorCode.PARAM_ERROR,"标签名称不合法");return name;}
    private KnowledgeTagVO toTagVO(KnowledgeTag tag){return new KnowledgeTagVO(String.valueOf(tag.getId()),tag.getName(),tag.getArticleCount()==null?0:tag.getArticleCount());}
    private String remark(KnowledgeActionRequest request){if(request==null)return "";String value=StringUtils.hasText(request.getReason())?request.getReason():request.getPublishRemark();return StringUtils.hasText(value)?"，备注："+value.trim():"";}
    private void audit(Long operatorId,String operation,Long bizId,String content,String ip,String ua){auditLogService.record(operatorId,operation,"KNOWLEDGE",bizId,content,ip,ua);}
}
