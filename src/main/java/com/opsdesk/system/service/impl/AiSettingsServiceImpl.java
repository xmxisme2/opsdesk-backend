package com.opsdesk.system.service.impl;

import com.opsdesk.ai.service.AiHealthProxyService;
import com.opsdesk.ai.vo.AiServiceHealthVO;
import com.opsdesk.common.security.CurrentUser;
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

    private final AiHealthProxyService healthProxyService;

    public AiSettingsServiceImpl(AiHealthProxyService healthProxyService) {
        this.healthProxyService = healthProxyService;
    }

    @Override
    public AiSettingsVO detail(CurrentUser currentUser) {
        AiServiceHealthVO health = healthProxyService.check(currentUser);
        return new AiSettingsVO(health.aiEnabled(), PROVIDER, MODEL, health.ragEnabled(), DISCLAIMER);
    }
}
