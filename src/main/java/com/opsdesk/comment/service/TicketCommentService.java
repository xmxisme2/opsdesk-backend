package com.opsdesk.comment.service;

import com.opsdesk.comment.dto.CommentCreateRequest;
import com.opsdesk.comment.dto.CommentSearchRequest;
import com.opsdesk.comment.vo.CommentVO;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;

/**
 * 工单评论服务。
 *
 * <p>负责评论新增、列表、逻辑删除、内部备注可见性和评论附件绑定。</p>
 */
public interface TicketCommentService {

    CommentVO create(String ticketId,
                     CommentCreateRequest request,
                     CurrentUser currentUser,
                     String requestIp,
                     String userAgent);

    PageResult<CommentVO> search(String ticketId,
                                 CommentSearchRequest request,
                                 CurrentUser currentUser);

    void delete(String id,
                CurrentUser currentUser,
                String requestIp,
                String userAgent);
}
