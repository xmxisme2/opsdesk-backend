package com.opsdesk.system.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.util.IdParser;
import com.opsdesk.system.dto.NotificationTemplateSearchRequest;
import com.opsdesk.system.dto.NotificationTemplateUpdateRequest;
import com.opsdesk.system.entity.NotificationTemplate;
import com.opsdesk.system.mapper.NotificationTemplateMapper;
import com.opsdesk.system.service.NotificationTemplateService;
import com.opsdesk.system.vo.NotificationTemplateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 通知模板服务实现，只允许业务已提供值的占位变量，避免保存后产生无法渲染的通知。 */
@Service
public class NotificationTemplateServiceImpl implements NotificationTemplateService {
    private static final Set<String> TYPES = Set.of("TICKET_ASSIGNED", "TICKET_COMMENTED", "TICKET_STATUS_CHANGED", "TICKET_OVERDUE", "TICKET_CLOSED");
    private static final Set<String> CHANNELS = Set.of("IN_APP", "EMAIL");
    private static final Map<String, List<String>> VARIABLES = Map.of(
            "TICKET_ASSIGNED", List.of("ticketNo", "teamName", "operatorName"),
            "TICKET_COMMENTED", List.of("ticketNo", "commenter"),
            "TICKET_STATUS_CHANGED", List.of("ticketNo", "status", "operatorName"),
            "TICKET_OVERDUE", List.of("ticketNo", "assignee"),
            "TICKET_CLOSED", List.of("ticketNo")
    );
    /** 模板变量中文说明：用于管理端解释变量含义，不参与实际模板替换。 */
    private static final Map<String, String> VARIABLE_DESCRIPTIONS = Map.of(
            "ticketNo", "工单编号",
            "assignee", "当前处理人姓名",
            "teamName", "工单所属处理组名称",
            "operatorName", "执行本次分派或状态变更的操作人姓名",
            "commenter", "发表评论的用户姓名",
            "status", "变更后的工单状态中文名称"
    );
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([A-Za-z][A-Za-z0-9]*)}");
    private final NotificationTemplateMapper mapper;
    private final AuditLogService auditLogService;
    public NotificationTemplateServiceImpl(NotificationTemplateMapper mapper, AuditLogService auditLogService) { this.mapper = mapper; this.auditLogService = auditLogService; }

    @Override public List<NotificationTemplateVO> search(NotificationTemplateSearchRequest request) {
        String type = normalize(request == null ? null : request.type(), TYPES, "通知类型");
        String channel = normalize(request == null ? null : request.channel(), CHANNELS, "通知渠道");
        return mapper.search(type, channel).stream().map(this::toVO).toList();
    }

    @Override @Transactional
    public NotificationTemplateVO update(String id, NotificationTemplateUpdateRequest request, Long operatorId, String requestIp, String userAgent) {
        Long templateId = IdParser.parseRequired(id, "通知模板ID");
        NotificationTemplate template = mapper.findById(templateId);
        if (template == null) throw new BusinessException(ErrorCode.NOT_FOUND, "通知模板不存在");
        String title = request.titleTemplate().trim();
        String content = request.contentTemplate().trim();
        validateVariables(template.getType(), title); validateVariables(template.getType(), content);
        template.setTitleTemplate(title); template.setContentTemplate(content); template.setEnabled(request.enabled() ? 1 : 0); template.setUpdateBy(operatorId);
        if (mapper.update(template) != 1) throw new BusinessException(ErrorCode.STATE_CONFLICT, "通知模板更新失败");
        auditLogService.record(operatorId, "UPDATE", "SYSTEM_CONFIG", templateId, "更新通知模板：" + template.getType(), requestIp, userAgent);
        return toVO(mapper.findById(templateId));
    }

    private void validateVariables(String type, String text) {
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        List<String> allowed = VARIABLES.getOrDefault(type, List.of());
        while (matcher.find()) if (!allowed.contains(matcher.group(1))) throw new BusinessException(ErrorCode.PARAM_ERROR, "模板包含不支持的变量：" + matcher.group());
        if (text.contains("{") && !VARIABLE_PATTERN.matcher(text).find()) throw new BusinessException(ErrorCode.PARAM_ERROR, "模板变量格式不正确");
    }
    private String normalize(String value, Set<String> allowed, String name) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw new BusinessException(ErrorCode.PARAM_ERROR, name + "不正确");
        return normalized;
    }
    private NotificationTemplateVO toVO(NotificationTemplate value) {
        List<String> variables = VARIABLES.getOrDefault(value.getType(), List.of());
        Map<String, String> descriptions = new LinkedHashMap<>();
        variables.forEach(variable -> descriptions.put(variable, VARIABLE_DESCRIPTIONS.getOrDefault(variable, variable)));
        return new NotificationTemplateVO(String.valueOf(value.getId()), value.getType(), value.getChannel(),
                value.getTitleTemplate(), value.getContentTemplate(), value.getEnabled() != null && value.getEnabled() == 1,
                variables, descriptions, value.getUpdateTime());
    }
}
