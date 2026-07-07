package com.opsdesk.dashboard.service;

import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.dashboard.vo.WorkbenchSummaryVO;

/**
 * 工作台服务。
 *
 * <p>负责首页待办指标、最近工单和最近通知摘要，所有数据按当前登录用户收敛。</p>
 */
public interface WorkbenchService {

    WorkbenchSummaryVO summary(String teamId, CurrentUser currentUser);
}
