package com.opsdesk.comment.vo;

import com.opsdesk.attachment.vo.AttachmentVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单评论响应对象。
 *
 * <p>ID 统一按字符串返回；附件只返回当前用户有权读取的评论附件。</p>
 */
public record CommentVO(
        String id,
        String ticketId,
        String content,
        String commentType,
        String authorId,
        String authorName,
        List<AttachmentVO> attachments,
        Boolean deleted,
        LocalDateTime createdAt
) {
}
