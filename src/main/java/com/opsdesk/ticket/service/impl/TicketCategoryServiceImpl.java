package com.opsdesk.ticket.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.team.entity.Team;
import com.opsdesk.team.mapper.TeamMapper;
import com.opsdesk.ticket.converter.TicketConverter;
import com.opsdesk.ticket.dto.TicketCategoryMutationRequest;
import com.opsdesk.ticket.dto.TicketCategoryTreeRequest;
import com.opsdesk.ticket.entity.TicketCategory;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import com.opsdesk.ticket.service.TicketCategoryService;
import com.opsdesk.ticket.vo.TicketCategoryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单分类服务实现。
 *
 * <p>集中处理分类树组装、同级唯一、父子循环、默认团队有效性、删除保护和配置审计。</p>
 */
@Service
public class TicketCategoryServiceImpl implements TicketCategoryService {

    /** 分类审计业务类型：分类配置变更统一归入 TICKET_CATEGORY，不允许外部传入。 */
    private static final String AUDIT_BIZ_TYPE = "TICKET_CATEGORY";

    /** 创建分类审计操作类型：管理员新增分类时使用，不允许外部传入。 */
    private static final String AUDIT_OPERATION_CREATE = "TICKET_CATEGORY_CREATE";

    /** 编辑分类审计操作类型：管理员修改分类时使用，不允许外部传入。 */
    private static final String AUDIT_OPERATION_UPDATE = "TICKET_CATEGORY_UPDATE";

    /** 删除分类审计操作类型：管理员逻辑删除分类时使用，不允许外部传入。 */
    private static final String AUDIT_OPERATION_DELETE = "TICKET_CATEGORY_DELETE";

    private final TicketCategoryMapper ticketCategoryMapper;
    private final TeamMapper teamMapper;
    private final TicketConverter ticketConverter;
    private final SnowflakeIdGenerator idGenerator;
    private final AuditLogService auditLogService;

    public TicketCategoryServiceImpl(TicketCategoryMapper ticketCategoryMapper,
                                     TeamMapper teamMapper,
                                     TicketConverter ticketConverter,
                                     SnowflakeIdGenerator idGenerator,
                                     AuditLogService auditLogService) {
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.teamMapper = teamMapper;
        this.ticketConverter = ticketConverter;
        this.idGenerator = idGenerator;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<TicketCategoryVO> tree(TicketCategoryTreeRequest request) {
        TicketCategoryTreeRequest safeRequest = request == null ? new TicketCategoryTreeRequest() : request;
        Integer enabled = safeRequest.getEnabled() == null ? null : (safeRequest.getEnabled() ? 1 : 0);
        List<TicketCategory> categories = ticketCategoryMapper.searchTree(enabled, safeRequest.normalizedKeyword());
        Set<Long> visibleIds = categories.stream().map(TicketCategory::getId).collect(Collectors.toSet());
        Map<Long, List<TicketCategory>> childrenByParentId = categories.stream()
                .collect(Collectors.groupingBy(category -> category.getParentId() == null ? 0L : category.getParentId()));
        return categories.stream()
                .filter(category -> category.getParentId() == null || !visibleIds.contains(category.getParentId()))
                .map(category -> toTreeVO(category, childrenByParentId))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketCategoryVO create(TicketCategoryMutationRequest request,
                                   Long operatorId,
                                   String requestIp,
                                   String userAgent) {
        Long parentId = parseOptionalId(request.getParentId(), "父级分类ID");
        if (parentId != null) {
            loadCategory(parentId, "父级分类不存在");
        }
        Team defaultTeam = validateDefaultTeam(request.getDefaultTeamId());
        validateDefaultSla(request.getDefaultSlaHours());
        String name = normalizeName(request.getName());
        if (ticketCategoryMapper.countByParentAndName(parentId, name, null) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "同级工单分类名称已存在");
        }

        TicketCategory category = new TicketCategory();
        category.setId(idGenerator.nextId());
        applyMutation(category, request, parentId, defaultTeam, operatorId);
        category.setCreateBy(operatorId);
        ticketCategoryMapper.insert(category);
        auditLogService.record(operatorId, AUDIT_OPERATION_CREATE, AUDIT_BIZ_TYPE, category.getId(),
                "创建工单分类：" + category.getName(), requestIp, userAgent);
        return ticketConverter.toCategoryVO(category, defaultTeam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketCategoryVO update(String id,
                                   TicketCategoryMutationRequest request,
                                   Long operatorId,
                                   String requestIp,
                                   String userAgent) {
        Long categoryId = IdParser.parseRequired(id, "工单分类ID");
        TicketCategory category = loadCategory(categoryId, "工单分类不存在");
        Long parentId = parseOptionalId(request.getParentId(), "父级分类ID");
        validateParent(categoryId, parentId);
        Team defaultTeam = validateDefaultTeam(request.getDefaultTeamId());
        validateDefaultSla(request.getDefaultSlaHours());
        String name = normalizeName(request.getName());
        if (ticketCategoryMapper.countByParentAndName(parentId, name, categoryId) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "同级工单分类名称已存在");
        }

        applyMutation(category, request, parentId, defaultTeam, operatorId);
        if (ticketCategoryMapper.update(category) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单分类不存在");
        }
        auditLogService.record(operatorId, AUDIT_OPERATION_UPDATE, AUDIT_BIZ_TYPE, categoryId,
                "编辑工单分类：" + category.getName(), requestIp, userAgent);
        return ticketConverter.toCategoryVO(category, defaultTeam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, Long operatorId, String requestIp, String userAgent) {
        Long categoryId = IdParser.parseRequired(id, "工单分类ID");
        TicketCategory category = loadCategory(categoryId, "工单分类不存在");
        if (ticketCategoryMapper.countChildren(categoryId) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "存在子分类，不能删除");
        }
        if (ticketCategoryMapper.countTickets(categoryId) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "分类存在关联工单，不能删除");
        }
        if (ticketCategoryMapper.logicalDelete(categoryId, operatorId) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单分类不存在");
        }
        auditLogService.record(operatorId, AUDIT_OPERATION_DELETE, AUDIT_BIZ_TYPE, categoryId,
                "删除工单分类：" + category.getName(), requestIp, userAgent);
    }

    private TicketCategoryVO toTreeVO(TicketCategory category, Map<Long, List<TicketCategory>> childrenByParentId) {
        List<TicketCategoryVO> children = childrenByParentId.getOrDefault(category.getId(), List.of())
                .stream()
                .map(child -> toTreeVO(child, childrenByParentId))
                .toList();
        Team defaultTeam = category.getDefaultTeamId() == null ? null : teamMapper.findById(category.getDefaultTeamId());
        return ticketConverter.toCategoryVO(category, defaultTeam, children);
    }

    private void validateParent(Long categoryId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (categoryId.equals(parentId)) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "父级分类不能选择自身");
        }
        if (ticketCategoryMapper.countDescendantRelation(categoryId, parentId) > 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "父级分类不能选择当前分类的子分类");
        }
        loadCategory(parentId, "父级分类不存在");
    }

    private Team validateDefaultTeam(String teamIdValue) {
        Long teamId = parseOptionalId(teamIdValue, "默认团队ID");
        if (teamId == null) {
            return null;
        }
        Team team = teamMapper.findById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "默认团队不存在");
        }
        if (team.getEnabled() == null || team.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "默认团队已停用");
        }
        return team;
    }

    private void validateDefaultSla(Integer defaultSlaHours) {
        if (defaultSlaHours != null && defaultSlaHours < 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "默认 SLA 必须为正整数小时");
        }
    }

    private void applyMutation(TicketCategory category,
                               TicketCategoryMutationRequest request,
                               Long parentId,
                               Team defaultTeam,
                               Long operatorId) {
        category.setParentId(parentId);
        category.setName(normalizeName(request.getName()));
        category.setDefaultTeamId(defaultTeam == null ? null : defaultTeam.getId());
        category.setDefaultSlaHours(request.getDefaultSlaHours());
        category.setSort(request.getSort());
        category.setEnabled(Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0);
        category.setUpdateBy(operatorId);
    }

    private TicketCategory loadCategory(Long categoryId, String message) {
        TicketCategory category = ticketCategoryMapper.findById(categoryId);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, message);
        }
        return category;
    }

    private Long parseOptionalId(String value, String fieldName) {
        return StringUtils.hasText(value) ? IdParser.parseRequired(value, fieldName) : null;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
