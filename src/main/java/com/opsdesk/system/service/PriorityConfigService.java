package com.opsdesk.system.service;

import com.opsdesk.system.dto.PriorityConfigUpdateRequest;
import com.opsdesk.system.vo.PriorityOptionVO;

import java.util.List;

/** 工单固定优先级配置服务，统一提供选项读取和管理员整体更新能力。 */
public interface PriorityConfigService {
    /** 读取固定四项优先级；缺失或损坏的单项使用内置默认值。 */
    List<PriorityOptionVO> options();

    /** 校验并整体更新固定四项优先级。 */
    List<PriorityOptionVO> update(PriorityConfigUpdateRequest request,
                                  Long operatorId,
                                  String requestIp,
                                  String userAgent);
}
