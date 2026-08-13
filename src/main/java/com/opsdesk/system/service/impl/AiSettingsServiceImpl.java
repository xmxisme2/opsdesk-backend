package com.opsdesk.system.service.impl;

import com.opsdesk.ai.service.AiRuntimeConfigProxyService;
import com.opsdesk.ai.vo.AiRuntimeConfigVO;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.system.dto.AiSettingsUpdateRequest;
import com.opsdesk.system.service.AiSettingsService;
import com.opsdesk.system.vo.AiSettingsVO;
import org.springframework.stereotype.Service;

/**
 * AI 公开运行设置实现。
 *
 * <p>开关从独立 AI 服务实时读取；供应商和模型为当前生产基线，密钥始终由服务端环境管理。</p>
 */
@Service
public class AiSettingsServiceImpl implements AiSettingsService {
    /** 当前 Chat 供应商名称，仅用于管理页面展示，不允许外部修改。 */
    private static final String PROVIDER = "DeepSeek";
    /** 当前生产 Chat 模型编码，仅用于管理页面展示，不允许外部修改。 */
    private static final String MODEL = "deepseek-v4-flash";
    /** AI 输出统一免责声明，保持与独立 AI 服务回答一致。 */
    private static final String DISCLAIMER = "AI 回答仅供参考，请以实际系统状态和知识文章为准。";

    private final AiRuntimeConfigProxyService configProxyService;
    private final AuditLogService auditLogService;

    public AiSettingsServiceImpl(AiRuntimeConfigProxyService configProxyService, AuditLogService auditLogService) {
        this.configProxyService = configProxyService;
        this.auditLogService = auditLogService;
    }

    @Override
    public AiSettingsVO detail(CurrentUser currentUser) {
        return toView(configProxyService.detail(currentUser));
    }

    @Override
    public AiSettingsVO update(AiSettingsUpdateRequest request, CurrentUser currentUser,
                               String requestIp, String userAgent) {
        AiRuntimeConfigVO config = configProxyService.update(request.enabled(), request.ragEnabled(), currentUser);
        String content = "更新 AI 运行开关：AI=" + display(config.enabled()) + "，RAG=" + display(config.ragEnabled());
        auditLogService.record(currentUser.getUserId(), "UPDATE", "SYSTEM_CONFIG", null, content, requestIp, userAgent);
        return toView(config);
    }

    private AiSettingsVO toView(AiRuntimeConfigVO config) {
        return new AiSettingsVO(config.enabled(), PROVIDER, MODEL, config.ragEnabled(),
                config.effectiveEnabled(), config.effectiveRagEnabled(), config.environmentEnabled(),
                config.environmentRagEnabled(), DISCLAIMER, config.updateTime());
    }

    private String display(boolean enabled) {
        return enabled ? "开启" : "关闭";
    }
}
