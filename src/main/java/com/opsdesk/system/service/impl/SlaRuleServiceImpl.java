package com.opsdesk.system.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.system.converter.SlaRuleConverter;
import com.opsdesk.system.dto.SlaRuleMutationRequest;
import com.opsdesk.system.dto.SlaRuleSearchRequest;
import com.opsdesk.system.entity.SlaRule;
import com.opsdesk.system.mapper.SlaRuleMapper;
import com.opsdesk.system.service.SlaRuleService;
import com.opsdesk.system.vo.SlaRuleVO;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** SLA 规则服务实现，集中处理组合唯一性、时限关系和审计日志。 */
@Service
public class SlaRuleServiceImpl implements SlaRuleService {
    /** 可由外部配置的固定工单优先级编码。 */
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");
    private final SlaRuleMapper mapper;
    private final TicketCategoryMapper categoryMapper;
    private final AuditLogService auditLogService;
    private final SnowflakeIdGenerator idGenerator;
    private final SlaRuleConverter converter;

    public SlaRuleServiceImpl(SlaRuleMapper mapper, TicketCategoryMapper categoryMapper,
                              AuditLogService auditLogService, SnowflakeIdGenerator idGenerator,
                              SlaRuleConverter converter) {
        this.mapper = mapper;
        this.categoryMapper = categoryMapper;
        this.auditLogService = auditLogService;
        this.idGenerator = idGenerator;
        this.converter = converter;
    }

    @Override
    public List<SlaRuleVO> search(SlaRuleSearchRequest request) {
        Long categoryId = request != null && StringUtils.hasText(request.getCategoryId())
                ? IdParser.parseRequired(request.getCategoryId(), "分类ID") : null;
        String priority = normalizePriority(request == null ? null : request.getPriority(), false);
        Integer enabled = request == null || request.getEnabled() == null ? null : (request.getEnabled() ? 1 : 0);
        return mapper.search(categoryId, priority, enabled).stream().map(converter::toVO).toList();
    }

    @Override
    @Transactional
    public SlaRuleVO create(SlaRuleMutationRequest request, Long operatorId, String requestIp, String userAgent) {
        SlaRule rule = buildRule(null, request, operatorId);
        ensureUnique(rule.getCategoryId(), rule.getPriority(), null);
        rule.setId(idGenerator.nextId());
        mapper.insert(rule);
        auditLogService.record(operatorId, "CREATE", "SYSTEM_CONFIG", rule.getId(),
                "创建 SLA 规则：" + rule.getPriority(), requestIp, userAgent);
        return converter.toVO(mapper.findById(rule.getId()));
    }

    @Override
    @Transactional
    public SlaRuleVO update(String id, SlaRuleMutationRequest request, Long operatorId, String requestIp, String userAgent) {
        Long ruleId = IdParser.parseRequired(id, "SLA规则ID");
        requireRule(ruleId);
        SlaRule rule = buildRule(ruleId, request, operatorId);
        ensureUnique(rule.getCategoryId(), rule.getPriority(), ruleId);
        mapper.update(rule);
        auditLogService.record(operatorId, "UPDATE", "SYSTEM_CONFIG", ruleId,
                "更新 SLA 规则：" + rule.getPriority(), requestIp, userAgent);
        return converter.toVO(mapper.findById(ruleId));
    }

    @Override
    @Transactional
    public void delete(String id, Long operatorId, String requestIp, String userAgent) {
        Long ruleId = IdParser.parseRequired(id, "SLA规则ID");
        requireRule(ruleId);
        mapper.disable(ruleId, operatorId);
        auditLogService.record(operatorId, "UPDATE", "SYSTEM_CONFIG", ruleId,
                "禁用 SLA 规则", requestIp, userAgent);
    }

    private SlaRule buildRule(Long id, SlaRuleMutationRequest request, Long operatorId) {
        Long categoryId = IdParser.parseRequired(request.getCategoryId(), "分类ID");
        if (categoryMapper.findById(categoryId) == null) throw new BusinessException(ErrorCode.NOT_FOUND, "工单分类不存在");
        if (request.getResolveHours() < request.getResponseHours()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "解决时限不能小于响应时限");
        }
        SlaRule rule = new SlaRule();
        rule.setId(id);
        rule.setCategoryId(categoryId);
        rule.setPriority(normalizePriority(request.getPriority(), true));
        rule.setResponseHours(request.getResponseHours());
        rule.setResolveHours(request.getResolveHours());
        rule.setEnabled(Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0);
        rule.setCreateBy(operatorId);
        rule.setUpdateBy(operatorId);
        return rule;
    }

    private String normalizePriority(String priority, boolean required) {
        if (!StringUtils.hasText(priority)) {
            if (required) throw new BusinessException(ErrorCode.PARAM_ERROR, "优先级不能为空");
            return null;
        }
        String normalized = priority.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PRIORITIES.contains(normalized)) throw new BusinessException(ErrorCode.PARAM_ERROR, "优先级不正确");
        return normalized;
    }

    private void ensureUnique(Long categoryId, String priority, Long excludeId) {
        if (mapper.findByCategoryAndPriority(categoryId, priority, excludeId) != null) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "同分类和优先级的 SLA 规则已存在");
        }
    }

    private SlaRule requireRule(Long id) {
        SlaRule rule = mapper.findById(id);
        if (rule == null) throw new BusinessException(ErrorCode.NOT_FOUND, "SLA 规则不存在");
        return rule;
    }
}
