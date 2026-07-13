package com.opsdesk.system.converter;

import com.opsdesk.system.entity.SlaRule;
import com.opsdesk.system.vo.SlaRuleVO;
import org.springframework.stereotype.Component;

/** SLA 规则实体与接口对象转换器。 */
@Component
public class SlaRuleConverter {
    public SlaRuleVO toVO(SlaRule rule) {
        return new SlaRuleVO(
                String.valueOf(rule.getId()),
                String.valueOf(rule.getCategoryId()),
                rule.getPriority(),
                rule.getResponseHours(),
                rule.getResolveHours(),
                rule.getEnabled() != null && rule.getEnabled() == 1,
                rule.getCreateTime(),
                rule.getUpdateTime()
        );
    }
}
