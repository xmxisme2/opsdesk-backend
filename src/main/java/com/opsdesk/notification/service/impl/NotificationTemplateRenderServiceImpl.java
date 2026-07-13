package com.opsdesk.notification.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.notification.model.RenderedNotification;
import com.opsdesk.notification.service.NotificationTemplateRenderService;
import com.opsdesk.system.entity.NotificationTemplate;
import com.opsdesk.system.mapper.NotificationTemplateMapper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 模板渲染实现，逐个替换白名单校验后的变量，并阻止未解析占位符进入通知表。 */
@Service
public class NotificationTemplateRenderServiceImpl implements NotificationTemplateRenderService {
    /** 当前实际发送渠道固定为站内通知，邮件模板不参与本轮渲染。 */
    private static final String CHANNEL_IN_APP = "IN_APP";
    /** 模板变量仅允许字母开头的驼峰名称，用于识别待替换占位符。 */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([A-Za-z][A-Za-z0-9]*)}");

    /** 通知模板数据访问入口，按通知类型和站内渠道读取当前模板。 */
    private final NotificationTemplateMapper mapper;

    public NotificationTemplateRenderServiceImpl(NotificationTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<RenderedNotification> render(String type, Map<String, String> variables) {
        NotificationTemplate template = mapper.findByTypeAndChannel(type, CHANNEL_IN_APP);
        if (template == null) throw new BusinessException(ErrorCode.NOT_FOUND, "站内通知模板不存在：" + type);
        if (template.getEnabled() == null || template.getEnabled() != 1) return Optional.empty();
        Map<String, String> safeVariables = variables == null ? Map.of() : variables;
        return Optional.of(new RenderedNotification(
                renderText(template.getTitleTemplate(), safeVariables),
                renderText(template.getContentTemplate(), safeVariables)
        ));
    }

    /**
     * 替换单段模板文本；变量缺失直接阻断写库，避免用户在通知中心看到原始占位符。
     */
    private String renderText(String template, Map<String, String> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String value = variables.get(matcher.group(1));
            if (value == null) throw new BusinessException(ErrorCode.STATE_CONFLICT, "通知模板变量缺少值：" + matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        if (VARIABLE_PATTERN.matcher(result).find()) throw new BusinessException(ErrorCode.STATE_CONFLICT, "通知模板存在未解析变量");
        return result.toString();
    }
}
