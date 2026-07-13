package com.opsdesk.system.service;

import com.opsdesk.system.dto.SlaRuleMutationRequest;
import com.opsdesk.system.dto.SlaRuleSearchRequest;
import com.opsdesk.system.vo.SlaRuleVO;

import java.util.List;

/** SLA 规则业务服务。 */
public interface SlaRuleService {
    List<SlaRuleVO> search(SlaRuleSearchRequest request);
    SlaRuleVO create(SlaRuleMutationRequest request, Long operatorId, String requestIp, String userAgent);
    SlaRuleVO update(String id, SlaRuleMutationRequest request, Long operatorId, String requestIp, String userAgent);
    void delete(String id, Long operatorId, String requestIp, String userAgent);
}
