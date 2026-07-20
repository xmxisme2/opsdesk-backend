package com.opsdesk.audit.service.impl;

import com.opsdesk.audit.entity.AuditLog;
import com.opsdesk.audit.converter.AuditLogConverter;
import com.opsdesk.audit.dto.AuditLogSearchRequest;
import com.opsdesk.audit.mapper.AuditLogMapper;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.audit.vo.AuditLogVO;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.id.SnowflakeIdGenerator;
import com.opsdesk.common.pagination.PageHelperPageResult;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.util.IdParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * 审计日志服务实现。
 *
 * <p>审计记录失败不阻断主流程，但会写入应用日志，避免登录注册因日志表异常全部不可用。</p>
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    /** 审计日志服务记录器：审计写库失败时只写应用日志，不阻断主业务流程。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    /** 审计文件记录器：仅输出操作元数据，不输出正文、令牌等潜在敏感内容。 */
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");

    private final AuditLogMapper auditLogMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final AuditLogConverter auditLogConverter;

    @Autowired
    public AuditLogServiceImpl(AuditLogMapper auditLogMapper, SnowflakeIdGenerator idGenerator) {
        this(auditLogMapper, idGenerator, new AuditLogConverter());
    }

    public AuditLogServiceImpl(AuditLogMapper auditLogMapper,
                               SnowflakeIdGenerator idGenerator,
                               AuditLogConverter auditLogConverter) {
        this.auditLogMapper = auditLogMapper;
        this.idGenerator = idGenerator;
        this.auditLogConverter = auditLogConverter;
    }

    @Override
    public PageResult<AuditLogVO> search(AuditLogSearchRequest request) {
        AuditLogSearchRequest safeRequest = request == null ? new AuditLogSearchRequest() : request;
        Long operatorId = parseOptionalId(safeRequest.getOperatorId(), "操作人ID");
        Long bizId = parseOptionalId(safeRequest.getBizId(), "业务ID");
        String operationType = normalizeCode(safeRequest.getOperationType());
        String bizType = normalizeCode(safeRequest.getBizType());
        String keyword = normalizeKeyword(safeRequest.getKeyword());
        LocalDateTime dateFrom = parseDateTime(safeRequest.getDateFrom(), false, "开始时间");
        LocalDateTime dateTo = parseDateTime(safeRequest.getDateTo(), true, "结束时间");
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间不能晚于结束时间");
        }
        return PageHelperPageResult.selectPage(
                safeRequest,
                () -> auditLogMapper.search(operatorId, operationType, bizType, bizId, dateFrom, dateTo, keyword),
                auditLogConverter::toVO
        );
    }

    @Override
    public void record(Long operatorId,
                       String operationType,
                       String bizType,
                       Long bizId,
                       String content,
                       String requestIp,
                       String userAgent) {
        try {
            recordStrict(operatorId, operationType, bizType, bizId, content, requestIp, userAgent);
        } catch (Exception exception) {
            LOGGER.warn("写入审计日志失败 operationType={}, bizType={}, bizId={}", operationType, bizType, bizId, exception);
        }
    }

    /** 严格审计不吞掉 Mapper 异常，确保关键业务事务能够感知失败并回滚。 */
    @Override
    public void recordStrict(Long operatorId,
                             String operationType,
                             String bizType,
                             Long bizId,
                             String content,
                             String requestIp,
                             String userAgent) {
        int affectedRows = auditLogMapper.insert(
                buildAuditLog(operatorId, operationType, bizType, bizId, content, requestIp, userAgent));
        if (affectedRows != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "审计日志写入失败");
        }
        // 文件审计用于运维排障，与数据库审计记录互补；不记录 content、IP 和 User-Agent 等敏感明细。
        AUDIT_LOGGER.info("审计记录已写入 operatorId={}, operationType={}, bizType={}, bizId={}",
                operatorId, operationType, bizType, bizId);
    }

    /** 统一构建审计实体，保证尽力记录与严格记录的字段口径完全一致。 */
    private AuditLog buildAuditLog(Long operatorId,
                                   String operationType,
                                   String bizType,
                                   Long bizId,
                                   String content,
                                   String requestIp,
                                   String userAgent) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(idGenerator.nextId());
        auditLog.setOperatorId(operatorId);
        auditLog.setOperationType(operationType);
        auditLog.setBizType(bizType);
        auditLog.setBizId(bizId);
        auditLog.setContent(content);
        auditLog.setRequestIp(requestIp);
        auditLog.setUserAgent(userAgent);
        auditLog.setCreateBy(operatorId);
        auditLog.setUpdateBy(operatorId);
        return auditLog;
    }

    private Long parseOptionalId(String value, String fieldName) {
        return StringUtils.hasText(value) ? IdParser.parseRequired(value.trim(), fieldName) : null;
    }

    private String normalizeCode(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String normalizeKeyword(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 日期筛选兼容 yyyy-MM-dd 和 ISO 日期时间；结束日期自动包含当天 23:59:59。 */
    private LocalDateTime parseDateTime(String value, boolean endOfDay, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() == 10) {
                LocalDate date = LocalDate.parse(text);
                return LocalDateTime.of(date, endOfDay ? LocalTime.MAX : LocalTime.MIN);
            }
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "格式不正确");
        }
    }
}
