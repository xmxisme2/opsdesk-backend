package com.opsdesk.system.service;

import com.opsdesk.system.dto.NotificationTemplateSearchRequest;
import com.opsdesk.system.dto.NotificationTemplateUpdateRequest;
import com.opsdesk.system.vo.NotificationTemplateVO;

import java.util.List;

/** 通知模板服务，负责筛选、变量安全校验、更新和审计。 */
public interface NotificationTemplateService {
    List<NotificationTemplateVO> search(NotificationTemplateSearchRequest request);
    NotificationTemplateVO update(String id, NotificationTemplateUpdateRequest request, Long operatorId, String requestIp, String userAgent);
}
