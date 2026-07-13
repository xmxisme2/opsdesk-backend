package com.opsdesk.system.dto;

import lombok.Getter;
import lombok.Setter;

/** SLA 规则筛选请求。 */
@Getter
@Setter
public class SlaRuleSearchRequest {
    private String categoryId;
    private String priority;
    private Boolean enabled;
}
