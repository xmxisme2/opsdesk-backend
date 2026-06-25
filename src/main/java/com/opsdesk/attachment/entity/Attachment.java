package com.opsdesk.attachment.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 附件元数据实体。
 *
 * <p>映射 ticket_attachment 表，storagePath 仅供后端读取本地文件，禁止写入对外 VO。</p>
 */
@Getter
@Setter
public class Attachment {

    private Long id;
    private String bizType;
    private Long bizId;
    private String tempToken;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String extension;
    private Integer previewable;
    private String previewType;
    private Integer downloadOnly;
    private String storagePath;
    private Long uploaderId;
    private String uploaderName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer deleted;
}
