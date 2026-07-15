package com.opsdesk.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.system.dto.PriorityConfigUpdateRequest;
import com.opsdesk.system.entity.SystemConfig;
import com.opsdesk.system.mapper.SystemConfigMapper;
import com.opsdesk.system.service.PriorityConfigService;
import com.opsdesk.system.vo.PriorityOptionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 工单固定优先级配置服务实现。
 *
 * <p>编码集合属于核心工单协议，不允许配置扩展；数据库只保存可调整的名称、排序、颜色和启用状态。</p>
 */
@Service
public class PriorityConfigServiceImpl implements PriorityConfigService {
    /** 优先级配置分组：仅供后端读取 system_config，不允许外部传入。 */
    private static final String CONFIG_GROUP = "PRIORITY";
    /** 优先级配置键前缀：拼接固定编码形成唯一配置键，不允许外部传入。 */
    private static final String CONFIG_KEY_PREFIX = "priority.";
    /** 优先级审计业务类型：配置变更统一归入 SYSTEM_CONFIG，不允许外部传入。 */
    private static final String AUDIT_BIZ_TYPE = "SYSTEM_CONFIG";
    /** 六位十六进制颜色格式：更新和读取时均使用，外部颜色必须符合此格式。 */
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    /** 固定编码与默认展示配置，顺序同时作为读取响应的稳定顺序，不允许外部修改集合。 */
    private static final LinkedHashMap<String, PriorityOptionVO> DEFAULTS = createDefaults();

    private final SystemConfigMapper mapper;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public PriorityConfigServiceImpl(SystemConfigMapper mapper,
                                     AuditLogService auditLogService,
                                     ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PriorityOptionVO> options() {
        Map<String, String> storedValues = new HashMap<>();
        List<SystemConfig> configs = mapper.findByGroup(CONFIG_GROUP);
        if (configs != null) {
            for (SystemConfig config : configs) {
                if (config != null && config.getConfigKey() != null) {
                    storedValues.put(config.getConfigKey(), config.getConfigValue());
                }
            }
        }

        Map<String, PriorityOptionVO> result = new LinkedHashMap<>();
        DEFAULTS.forEach((code, fallback) -> result.put(code,
                parseStoredOption(code, storedValues.get(CONFIG_KEY_PREFIX + code), fallback)));
        fallbackDuplicateSorts(result);
        return result.values().stream().sorted(Comparator.comparing(PriorityOptionVO::sort)).toList();
    }

    @Override
    @Transactional
    public List<PriorityOptionVO> update(PriorityConfigUpdateRequest request,
                                         Long operatorId,
                                         String requestIp,
                                         String userAgent) {
        List<PriorityOptionVO> normalized = validateAndNormalize(request);
        for (PriorityOptionVO option : normalized) {
            updateRequired(CONFIG_KEY_PREFIX + option.code(), serialize(option), operatorId);
        }
        auditLogService.record(operatorId, "UPDATE", AUDIT_BIZ_TYPE, null,
                "更新工单优先级配置", requestIp, userAgent);
        return normalized.stream().sorted(Comparator.comparing(PriorityOptionVO::sort)).toList();
    }

    /**
     * 单项读取采用完整回退：JSON 无法解析或任一字段非法时，仅回退当前编码，其他编码继续使用存储值。
     */
    private PriorityOptionVO parseStoredOption(String code, String json, PriorityOptionVO fallback) {
        if (!StringUtils.hasText(json)) {
            return fallback;
        }
        try {
            PriorityValue value = objectMapper.readValue(json, PriorityValue.class);
            if (!isValidValue(value) || ("MEDIUM".equals(code) && !Boolean.TRUE.equals(value.enabled()))) {
                return fallback;
            }
            return new PriorityOptionVO(code, value.name().trim(), value.sort(),
                    value.color().toUpperCase(Locale.ROOT), value.enabled());
        } catch (JsonProcessingException | RuntimeException ignored) {
            return fallback;
        }
    }

    /** 排序冲突时只回退参与冲突的存储项；默认项本身排序固定且互不冲突。 */
    private void fallbackDuplicateSorts(Map<String, PriorityOptionVO> options) {
        boolean changed;
        do {
            changed = false;
            Map<Integer, List<String>> codesBySort = new HashMap<>();
            options.forEach((code, option) ->
                    codesBySort.computeIfAbsent(option.sort(), ignored -> new ArrayList<>()).add(code));
            for (List<String> duplicateCodes : codesBySort.values()) {
                if (duplicateCodes.size() < 2) {
                    continue;
                }
                for (String code : duplicateCodes) {
                    PriorityOptionVO fallback = DEFAULTS.get(code);
                    if (!options.get(code).equals(fallback)) {
                        options.put(code, fallback);
                        changed = true;
                    }
                }
            }
        } while (changed && hasDuplicateSort(options.values()));
    }

    private boolean hasDuplicateSort(Collection<PriorityOptionVO> options) {
        return options.stream().map(PriorityOptionVO::sort).distinct().count() != options.size();
    }

    /** 整体校验固定编码、必填字段、唯一排序和启用安全底线。 */
    private List<PriorityOptionVO> validateAndNormalize(PriorityConfigUpdateRequest request) {
        if (request == null || request.items() == null || request.items().size() != DEFAULTS.size()) {
            throw paramError("必须一次提交四个固定优先级");
        }

        Map<String, PriorityOptionVO> byCode = new HashMap<>();
        Set<Integer> sorts = new HashSet<>();
        boolean anyEnabled = false;
        for (PriorityOptionVO item : request.items()) {
            if (item == null || !DEFAULTS.containsKey(item.code()) || byCode.put(item.code(), item) != null) {
                throw paramError("优先级编码集合必须为 LOW、MEDIUM、HIGH、URGENT");
            }
            PriorityValue value = new PriorityValue(item.name(), item.sort(), item.color(), item.enabled());
            if (!isValidValue(value)) {
                throw paramError("优先级名称、排序、颜色或启用状态不合法");
            }
            if (!sorts.add(item.sort())) {
                throw paramError("优先级排序值不能重复");
            }
            anyEnabled |= item.enabled();
        }
        if (!byCode.keySet().equals(DEFAULTS.keySet())) {
            throw paramError("优先级编码集合必须为 LOW、MEDIUM、HIGH、URGENT");
        }
        if (!anyEnabled) {
            throw paramError("至少需要启用一个优先级");
        }
        if (!Boolean.TRUE.equals(byCode.get("MEDIUM").enabled())) {
            throw paramError("默认优先级 MEDIUM 必须启用");
        }

        return DEFAULTS.keySet().stream().map(code -> {
            PriorityOptionVO item = byCode.get(code);
            return new PriorityOptionVO(code, item.name().trim(), item.sort(),
                    item.color().toUpperCase(Locale.ROOT), item.enabled());
        }).toList();
    }

    private boolean isValidValue(PriorityValue value) {
        return value != null
                && StringUtils.hasText(value.name())
                && value.sort() != null
                && StringUtils.hasText(value.color())
                && COLOR_PATTERN.matcher(value.color()).matches()
                && value.enabled() != null;
    }

    /** 配置值只序列化四个可编辑字段，固定编码保留在 system_config 的 key 中。 */
    private String serialize(PriorityOptionVO option) {
        try {
            return objectMapper.writeValueAsString(
                    new PriorityValue(option.name(), option.sort(), option.color(), option.enabled()));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "优先级配置序列化失败");
        }
    }

    private void updateRequired(String key, String value, Long operatorId) {
        if (mapper.updateValue(key, value, operatorId) != 1) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "优先级配置缺失或不可编辑：" + key);
        }
    }

    private BusinessException paramError(String message) {
        return new BusinessException(ErrorCode.PARAM_ERROR, message);
    }

    private static LinkedHashMap<String, PriorityOptionVO> createDefaults() {
        LinkedHashMap<String, PriorityOptionVO> defaults = new LinkedHashMap<>();
        defaults.put("LOW", new PriorityOptionVO("LOW", "低", 10, "#0D8052", true));
        defaults.put("MEDIUM", new PriorityOptionVO("MEDIUM", "中", 20, "#1252AD", true));
        defaults.put("HIGH", new PriorityOptionVO("HIGH", "高", 30, "#BA630F", true));
        defaults.put("URGENT", new PriorityOptionVO("URGENT", "紧急", 40, "#C71F24", true));
        return defaults;
    }

    /** system_config 中单项 JSON 结构，不包含由配置键承载的固定编码。 */
    private record PriorityValue(String name, Integer sort, String color, Boolean enabled) {
    }
}
