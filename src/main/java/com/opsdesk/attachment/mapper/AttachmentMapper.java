package com.opsdesk.attachment.mapper;

import com.opsdesk.attachment.entity.Attachment;
import com.opsdesk.attachment.model.AttachmentResourceInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 附件元数据访问 Mapper。
 *
 * <p>当前先提供评论和知识库权限投影查询，附件元数据读写方法随服务实现继续补充。</p>
 */
@Mapper
public interface AttachmentMapper {

    int insert(Attachment attachment);

    Attachment findById(@Param("id") Long id);

    List<Attachment> findByBiz(@Param("bizType") String bizType,
                               @Param("bizId") Long bizId);

    List<Attachment> findByTempToken(@Param("bizType") String bizType,
                                     @Param("tempToken") String tempToken,
                                     @Param("uploaderId") Long uploaderId);

    int bindTempToBiz(@Param("bizType") String bizType,
                      @Param("tempToken") String tempToken,
                      @Param("uploaderId") Long uploaderId,
                      @Param("bizId") Long bizId,
                      @Param("operatorId") Long operatorId);

    Long lockTicket(@Param("ticketId") Long ticketId);

    long countActiveByTicketScope(@Param("ticketId") Long ticketId);

    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);

    AttachmentResourceInfo findCommentResource(@Param("commentId") Long commentId);

    AttachmentResourceInfo findKnowledgeResource(@Param("articleId") Long articleId);
}
