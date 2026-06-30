package com.opsdesk.attachment.mapper;

import com.opsdesk.attachment.entity.Attachment;
import com.opsdesk.attachment.model.AttachmentResourceInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    @Select("""
            SELECT a.*,
                   COALESCE(NULLIF(u.nickname, ''), NULLIF(u.username, ''), u.phone) AS uploader_name
            FROM ticket_attachment a
            LEFT JOIN sys_user u ON u.id = a.uploader_id AND u.deleted = 0
            WHERE a.id = #{id}
              AND a.deleted = 0
            LIMIT 1
            """)
    Attachment findById(@Param("id") Long id);

    @Select("""
            SELECT a.*,
                   COALESCE(NULLIF(u.nickname, ''), NULLIF(u.username, ''), u.phone) AS uploader_name
            FROM ticket_attachment a
            LEFT JOIN sys_user u ON u.id = a.uploader_id AND u.deleted = 0
            WHERE a.biz_type = #{bizType}
              AND a.biz_id = #{bizId}
              AND a.deleted = 0
            ORDER BY a.create_time ASC, a.id ASC
            """)
    List<Attachment> findByBiz(@Param("bizType") String bizType,
                               @Param("bizId") Long bizId);

    @Select("""
            SELECT a.*,
                   COALESCE(NULLIF(u.nickname, ''), NULLIF(u.username, ''), u.phone) AS uploader_name
            FROM ticket_attachment a
            LEFT JOIN sys_user u ON u.id = a.uploader_id AND u.deleted = 0
            WHERE a.biz_type = #{bizType}
              AND a.biz_id IS NULL
              AND a.temp_token = #{tempToken}
              AND a.uploader_id = #{uploaderId}
              AND a.deleted = 0
            ORDER BY a.create_time ASC, a.id ASC
            """)
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

    @Select("""
            SELECT id
            FROM ticket
            WHERE id = #{ticketId}
              AND deleted = 0
            FOR UPDATE
            """)
    Long lockTicket(@Param("ticketId") Long ticketId);

    @Select("""
            SELECT COUNT(1)
            FROM ticket_attachment a
            WHERE a.deleted = 0
              AND (
                (a.biz_type = 'TICKET' AND a.biz_id = #{ticketId})
                OR (
                  a.biz_type = 'COMMENT'
                  AND EXISTS (
                    SELECT 1
                    FROM ticket_comment c
                    WHERE c.id = a.biz_id
                      AND c.ticket_id = #{ticketId}
                      AND c.deleted = 0
                  )
                )
              )
            """)
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

    @Select("""
            SELECT ticket_id,
                   author_id AS owner_id,
                   comment_type AS status
            FROM ticket_comment
            WHERE id = #{commentId}
              AND deleted = 0
            LIMIT 1
            """)
    AttachmentResourceInfo findCommentResource(@Param("commentId") Long commentId);

    @Select("""
            SELECT author_id AS owner_id,
                   status
            FROM knowledge_article
            WHERE id = #{articleId}
              AND deleted = 0
            LIMIT 1
            """)
    AttachmentResourceInfo findKnowledgeResource(@Param("articleId") Long articleId);
}
