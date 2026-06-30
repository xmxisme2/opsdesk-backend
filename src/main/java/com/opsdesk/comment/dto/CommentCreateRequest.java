package com.opsdesk.comment.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 新增评论请求。
 *
 * <p>commentType 支持 PUBLIC 和 INTERNAL；tempToken 用于把创建评论前上传的临时附件绑定到评论。</p>
 */
@Getter
@Setter
public class CommentCreateRequest {

    private String content;
    private String commentType;
    private String tempToken;
}
