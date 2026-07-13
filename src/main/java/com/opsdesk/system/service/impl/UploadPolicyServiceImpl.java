package com.opsdesk.system.service.impl;

import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.system.dto.UploadPolicyUpdateRequest;
import com.opsdesk.system.entity.SystemConfig;
import com.opsdesk.system.mapper.SystemConfigMapper;
import com.opsdesk.system.service.UploadPolicyService;
import com.opsdesk.system.vo.UploadPolicyVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 上传限制服务实现，配置范围不得超出服务器已经实现的 MIME 与文件签名安全校验。 */
@Service
public class UploadPolicyServiceImpl implements UploadPolicyService {
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf", "docx", "xlsx", "txt", "log", "zip");
    public static final Set<String> SAFE_PREVIEW_EXTENSIONS = Set.of("jpg", "jpeg", "png", "txt", "log");
    private static final String GROUP_UPLOAD = "UPLOAD";
    private static final String KEY_MAX_SIZE = "upload.max_file_size_mb";
    private static final String KEY_MAX_COUNT = "upload.max_files_per_ticket";
    private static final String KEY_ALLOWED = "upload.allowed_extensions";
    private static final String KEY_PREVIEWABLE = "upload.previewable_extensions";
    private static final String KEY_DOWNLOAD_ONLY = "upload.download_only_extensions";
    private final SystemConfigMapper mapper;
    private final AuditLogService auditLogService;

    public UploadPolicyServiceImpl(SystemConfigMapper mapper, AuditLogService auditLogService) {
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public UploadPolicyVO detail() {
        Map<String, String> values = new HashMap<>();
        for (SystemConfig config : mapper.findByGroup(GROUP_UPLOAD)) values.put(config.getConfigKey(), config.getConfigValue());
        return normalize(parseInt(values.get(KEY_MAX_SIZE), 20), parseInt(values.get(KEY_MAX_COUNT), 10),
                parseList(values.getOrDefault(KEY_ALLOWED, String.join(",", SUPPORTED_EXTENSIONS))),
                parseList(values.getOrDefault(KEY_PREVIEWABLE, "jpg,jpeg,png,txt,log")),
                parseList(values.getOrDefault(KEY_DOWNLOAD_ONLY, "pdf,docx,xlsx,zip")));
    }

    @Override
    @Transactional
    public UploadPolicyVO update(UploadPolicyUpdateRequest request, Long operatorId, String requestIp, String userAgent) {
        UploadPolicyVO policy = normalize(request.maxFileSizeMb(), request.maxFilesPerTicket(), request.allowedExtensions(),
                request.previewableExtensions(), request.downloadOnlyExtensions());
        updateRequired(KEY_MAX_SIZE, String.valueOf(policy.maxFileSizeMb()), operatorId);
        updateRequired(KEY_MAX_COUNT, String.valueOf(policy.maxFilesPerTicket()), operatorId);
        updateRequired(KEY_ALLOWED, String.join(",", policy.allowedExtensions()), operatorId);
        updateRequired(KEY_PREVIEWABLE, String.join(",", policy.previewableExtensions()), operatorId);
        updateRequired(KEY_DOWNLOAD_ONLY, String.join(",", policy.downloadOnlyExtensions()), operatorId);
        auditLogService.record(operatorId, "UPDATE", "SYSTEM_CONFIG", null, "更新附件上传限制", requestIp, userAgent);
        return policy;
    }

    private UploadPolicyVO normalize(int size, int count, List<String> allowedInput, List<String> previewInput, List<String> downloadInput) {
        if (size < 1 || size > 100 || count < 1 || count > 50) throw new BusinessException(ErrorCode.PARAM_ERROR, "上传限制超出允许范围");
        List<String> allowed = normalizeList(allowedInput);
        List<String> preview = normalizeList(previewInput);
        List<String> download = normalizeList(downloadInput);
        if (allowed.isEmpty() || !SUPPORTED_EXTENSIONS.containsAll(allowed)) throw new BusinessException(ErrorCode.PARAM_ERROR, "包含服务器不支持的扩展名");
        if (!SAFE_PREVIEW_EXTENSIONS.containsAll(preview)) throw new BusinessException(ErrorCode.PARAM_ERROR, "包含不支持安全预览的扩展名");
        Set<String> classified = new HashSet<>(preview);
        if (!Collections.disjoint(classified, download)) throw new BusinessException(ErrorCode.PARAM_ERROR, "预览与仅下载扩展名不能重复");
        classified.addAll(download);
        if (!classified.equals(new HashSet<>(allowed))) throw new BusinessException(ErrorCode.PARAM_ERROR, "每个允许扩展名必须且只能归入一种处理方式");
        return new UploadPolicyVO(size, count, allowed, preview, download);
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull).map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank()).distinct().sorted().toList();
    }
    private List<String> parseList(String value) { return normalizeList(Arrays.asList(value.split(","))); }
    private int parseInt(String value, int fallback) { try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; } }
    private void updateRequired(String key, String value, Long operatorId) {
        if (mapper.updateValue(key, value, operatorId) != 1) throw new BusinessException(ErrorCode.STATE_CONFLICT, "上传配置缺失或不可编辑：" + key);
    }
}
