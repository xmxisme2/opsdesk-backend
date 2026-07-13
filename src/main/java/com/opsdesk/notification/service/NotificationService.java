package com.opsdesk.notification.service;

import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.notification.dto.NotificationReadAllRequest;
import com.opsdesk.notification.dto.NotificationSearchRequest;
import com.opsdesk.notification.vo.NotificationReadAllVO;
import com.opsdesk.notification.vo.NotificationUnreadCountVO;
import com.opsdesk.notification.vo.NotificationVO;
import java.util.Map;

/**
 * 通知中心服务。
 *
 * <p>负责站内通知列表、未读数量、读状态变更和后续业务事件写入通知。</p>
 */
public interface NotificationService {

    PageResult<NotificationVO> search(NotificationSearchRequest request, CurrentUser currentUser);

    NotificationUnreadCountVO unreadCount(CurrentUser currentUser);

    NotificationVO markRead(String id, CurrentUser currentUser);

    NotificationReadAllVO readAll(NotificationReadAllRequest request, CurrentUser currentUser);

    /**
     * 创建工单站内通知，由业务服务在分派、评论和状态流转后调用。
     */
    void createTicketNotification(Long receiverId,
                                  String type,
                                  String title,
                                  String content,
                                  Long ticketId,
                                  Long operatorId);

    /** 按通知类型读取启用模板并渲染变量；模板停用时不创建消息。 */
    void createTicketNotification(Long receiverId, String type, Map<String, String> variables, Long ticketId, Long operatorId);
}
