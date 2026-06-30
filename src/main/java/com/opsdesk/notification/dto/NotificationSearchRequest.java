package com.opsdesk.notification.dto;

import com.opsdesk.common.pagination.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 通知列表查询请求。
 *
 * <p>只允许查询当前登录人的通知；receiverId 不从前端传入，避免越权查询。</p>
 */
@Getter
@Setter
public class NotificationSearchRequest extends PageQuery {

    private Boolean read;
    private String type;
    private String createdFrom;
    private String createdTo;
}
