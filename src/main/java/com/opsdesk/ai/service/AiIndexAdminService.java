package com.opsdesk.ai.service;

import com.opsdesk.ai.dto.IndexRebuildRequest;
import com.opsdesk.ai.dto.IndexReindexRequest;
import com.opsdesk.ai.dto.IndexReconcileRequest;
import com.opsdesk.ai.vo.IndexTaskAcceptedVO;
import com.opsdesk.common.security.CurrentUser;

/** 主应用知识索引管理服务。 */
public interface AiIndexAdminService {
    IndexTaskAcceptedVO reindex(String articleId, IndexReindexRequest request, CurrentUser currentUser);
    IndexTaskAcceptedVO rebuild(IndexRebuildRequest request, CurrentUser currentUser);
    IndexTaskAcceptedVO reconcile(IndexReconcileRequest request, CurrentUser currentUser);
}
