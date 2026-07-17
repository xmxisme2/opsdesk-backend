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

    /** 查询当前上传人指定的未绑定临时附件，供业务创建成功后做精确绑定。 */
    List<Attachment> findTemporaryByIds(@Param("bizType") String bizType,
                                        @Param("attachmentIds") List<Long> attachmentIds,
                                        @Param("uploaderId") Long uploaderId);

    /** 仅绑定已校验归属的临时附件，禁止把其他业务附件重新挂载到当前资源。 */
    int bindTemporaryByIds(@Param("bizType") String bizType,
                           @Param("attachmentIds") List<Long> attachmentIds,
                           @Param("uploaderId") Long uploaderId,
                           @Param("bizId") Long bizId,
                           @Param("operatorId") Long operatorId);

    Long lockTicket(@Param("ticketId") Long ticketId);

    long countActiveByTicketScope(@Param("ticketId") Long ticketId);

    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);

    /** 业务资源逻辑删除时同步隐藏其附件元数据，物理文件仍由后续清理任务回收。 */
    int logicalDeleteByBiz(@Param("bizType") String bizType,
                           @Param("bizId") Long bizId,
                           @Param("operatorId") Long operatorId);

    AttachmentResourceInfo findCommentResource(@Param("commentId") Long commentId);

    AttachmentResourceInfo findKnowledgeResource(@Param("articleId") Long articleId);
}
