package com.opsdesk.ai.service;

import com.opsdesk.ai.dto.AiQualityRangeRequest;
import com.opsdesk.ai.dto.AiQualitySampleSearchRequest;
import com.opsdesk.ai.vo.AiQualityOverviewVO;
import com.opsdesk.ai.vo.AiQualitySampleVO;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;

/** 主应用 AI 质量统计安全代理。 */
public interface AiQualityProxyService {
    AiQualityOverviewVO overview(AiQualityRangeRequest request, CurrentUser currentUser);
    PageResult<AiQualitySampleVO> searchSamples(AiQualitySampleSearchRequest request, CurrentUser currentUser);
}
