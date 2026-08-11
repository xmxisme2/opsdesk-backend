package com.opsdesk.system.service;

import com.opsdesk.system.dto.SystemConfigSearchRequest;
import com.opsdesk.system.vo.SystemConfigVO;

import java.util.List;

/** 系统通用配置查询服务。 */
public interface SystemConfigService {
    List<SystemConfigVO> search(SystemConfigSearchRequest request);
}
