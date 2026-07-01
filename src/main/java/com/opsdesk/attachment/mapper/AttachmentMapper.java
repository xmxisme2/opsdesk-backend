package com.opsdesk.attachment.mapper;

import com.opsdesk.attachment.entity.Attachment;
import com.opsdesk.attachment.model.AttachmentResourceInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 附件元数据访问 Mapper。
 *
 * <p>当前先提供评论和知识库权限投影查询，附件元数据读写方法随服务实现继续补充。</p>
 */
@Mapper
public interface AttachmentMapper {

    @Insert("""
            INSERT INTO ticket_attachment (
              id, biz_type, biz_id, temp_token, file_name, file_size, content_type, extension,
              previewable, preview_type, download_only, storage_path, uploader_id,
              create_by, update_by, deleted
            )
            VALUES (
              #{id}, #{bizType}, #{bizId}, #{tempToken}, #{fileName}, #{fileSize}, #{contentType}, #{extension},
              #{previewable}, #{previewType}, #{downloadOnly}, #{storagePath}, #{uploaderId},
              #{createBy}, #{updateBy}, 0
            )
            """)
    int insert(Attachment attachment);

    Attachment findById(@Param("id") Long id);

    List<Attachment> findByBiz(@Param("bizType") String bizType,
                               @Param("bizId") Long bizId);

    List<Attachment> findByTempToken(@Param("bizType") String bizType,
                                     @Param("tempToken") String tempToken,
                                     @Param("uploaderId") Long uploaderId);

    @Update("""
            UPDATE ticket_attachment
            SET biz_id = #{bizId},
                temp_token = NULL,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE biz_type = #{bizType}
              AND biz_id IS NULL
              AND temp_token = #{tempToken}
              AND uploader_id = #{uploaderId}
              AND deleted = 0
            """)
    int bindTempToBiz(@Param("bizType") String bizType,
                      @Param("tempToken") String tempToken,
                      @Param("uploaderId") Long uploaderId,
                      @Param("bizId") Long bizId,
                      @Param("operatorId") Long operatorId);

    Long lockTicket(@Param("ticketId") Long ticketId);

    long countActiveByTicketScope(@Param("ticketId") Long ticketId);

    @Update("""
            UPDATE ticket_attachment
            SET deleted = 1,
                update_by = #{operatorId},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
            """)
    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);

    AttachmentResourceInfo findCommentResource(@Param("commentId") Long commentId);

    AttachmentResourceInfo findKnowledgeResource(@Param("articleId") Long articleId);
}
