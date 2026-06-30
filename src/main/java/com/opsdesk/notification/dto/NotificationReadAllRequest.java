package com.opsdesk.notification.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 全部已读请求。
 *
 * <p>type 可选；传入时只标记当前用户指定类型通知，未传则标记当前用户全部未读通知。</p>
 */
@Getter
@Setter
public class NotificationReadAllRequest {

    private String type;
}
