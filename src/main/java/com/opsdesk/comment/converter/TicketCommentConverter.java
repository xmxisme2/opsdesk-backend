package com.opsdesk.comment.converter;

import com.opsdesk.attachment.vo.AttachmentVO;
import com.opsdesk.comment.entity.TicketComment;
import com.opsdesk.comment.vo.CommentVO;
import com.opsdesk.user.entity.SysUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工单评论对象转换器。
 *
 * <p>集中处理 Entity 到 VO 的字段命名、ID 字符串化和作者展示名兜底。</p>
 */
@Component
public class TicketCommentConverter {

    public CommentVO toVO(TicketComment comment, SysUser author, List<AttachmentVO> attachments) {
        if (comment == null) {
            return null;
        }
        return new CommentVO(
                String.valueOf(comment.getId()),
                String.valueOf(comment.getTicketId()),
                comment.getContent(),
                comment.getCommentType(),
                String.valueOf(comment.getAuthorId()),
                authorName(comment, author),
                attachments == null ? List.of() : attachments,
                comment.getDeleted() != null && comment.getDeleted() == 1,
                comment.getCreateTime()
        );
    }

    private String authorName(TicketComment comment, SysUser author) {
        if (StringUtils.hasText(comment.getAuthorName())) {
            return comment.getAuthorName();
        }
        if (author == null) {
            return null;
        }
        if (StringUtils.hasText(author.getNickname())) {
            return author.getNickname();
        }
        if (StringUtils.hasText(author.getUsername())) {
            return author.getUsername();
        }
        return author.getPhone();
    }
}
