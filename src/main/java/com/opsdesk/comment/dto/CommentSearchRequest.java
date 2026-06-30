package com.opsdesk.comment.dto;

import com.opsdesk.common.pagination.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 评论列表查询请求。
 *
 * <p>复用公共分页字段；是否展示内部备注由后端根据当前用户角色和工单资源范围计算，前端不可传参控制。</p>
 */
@Getter
@Setter
public class CommentSearchRequest extends PageQuery {
}
