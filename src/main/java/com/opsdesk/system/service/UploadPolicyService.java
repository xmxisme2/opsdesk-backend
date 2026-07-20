package com.opsdesk.system.service;

import com.opsdesk.system.dto.UploadPolicyUpdateRequest;
import com.opsdesk.system.vo.UploadPolicyVO;

/** 上传限制服务，统一向管理接口和附件校验提供当前有效策略。 */
public interface UploadPolicyService {
    UploadPolicyVO detail();
    UploadPolicyVO update(UploadPolicyUpdateRequest request, Long operatorId, String requestIp, String userAgent);
}
