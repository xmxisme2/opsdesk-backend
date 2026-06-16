package com.opsdesk.ticket.service;

import com.opsdesk.common.pagination.PageQuery;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.ticket.dto.TicketAssignRequest;
import com.opsdesk.ticket.dto.TicketCompleteRequest;
import com.opsdesk.ticket.dto.TicketCreateRequest;
import com.opsdesk.ticket.dto.TicketReasonRequest;
import com.opsdesk.ticket.dto.TicketSearchRequest;
import com.opsdesk.ticket.dto.TicketTransferRequest;
import com.opsdesk.ticket.dto.TicketUpdateRequest;
import com.opsdesk.ticket.vo.TicketListItemVO;
import com.opsdesk.ticket.vo.TicketOperationLogVO;
import com.opsdesk.ticket.vo.TicketVO;
import com.opsdesk.ticket.vo.TicketWatchVO;

/**
 * 工单主流程服务。
 *
 * <p>统一承接创建、查询、详情、状态动作、关注和操作日志，Controller 不直接修改工单状态。</p>
 */
public interface TicketService {

    TicketVO create(TicketCreateRequest request, CurrentUser currentUser, String requestIp, String userAgent);

    TicketVO updateDraft(String id, TicketUpdateRequest request, CurrentUser currentUser, String requestIp, String userAgent);

    PageResult<TicketListItemVO> search(TicketSearchRequest request, CurrentUser currentUser);

    TicketVO detail(String id, CurrentUser currentUser);

    TicketVO submit(String id, CurrentUser currentUser, String requestIp, String userAgent);

    TicketVO assign(String id, TicketAssignRequest request, CurrentUser currentUser, String requestIp, String userAgent);

    TicketVO reject(String id, TicketReasonRequest request, CurrentUser currentUser, String requestIp, String userAgent);

    TicketVO accept(String id, CurrentUser currentUser, String requestIp, String userAgent);

    TicketVO transfer(String id, TicketTransferRequest request, CurrentUser currentUser, String requestIp, String userAgent);

    TicketVO complete(String id, TicketCompleteRequest request, CurrentUser currentUser, String requestIp, String userAgent);

    TicketVO confirm(String id, TicketReasonRequest request, CurrentUser currentUser, String requestIp, String userAgent);

    TicketVO reopen(String id, TicketReasonRequest request, CurrentUser currentUser, String requestIp, String userAgent);

    TicketVO close(String id, TicketReasonRequest request, CurrentUser currentUser, String requestIp, String userAgent);

    TicketVO cancel(String id, TicketReasonRequest request, CurrentUser currentUser, String requestIp, String userAgent);

    TicketWatchVO watch(String id, CurrentUser currentUser, String requestIp, String userAgent);

    TicketWatchVO unwatch(String id, CurrentUser currentUser, String requestIp, String userAgent);

    PageResult<TicketOperationLogVO> searchOperationLogs(String id, PageQuery request, CurrentUser currentUser);
}
