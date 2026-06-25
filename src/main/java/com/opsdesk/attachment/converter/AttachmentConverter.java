package com.opsdesk.attachment.converter;

import com.opsdesk.attachment.entity.Attachment;
import com.opsdesk.attachment.vo.AttachmentVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 附件对象转换器。
 *
 * <p>集中执行 ID 字符串化、时间格式化和下载地址生成，明确排除后端 storagePath。</p>
 */
@Component
public class AttachmentConverter {

    /** 时间输出格式：与现有工单接口保持一致，便于前端统一渲染。 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AttachmentVO toVO(Attachment attachment) {
        return new AttachmentVO(
                String.valueOf(attachment.getId()),
                attachment.getBizType(),
                attachment.getBizId() == null ? null : String.valueOf(attachment.getBizId()),
                attachment.getTempToken(),
                attachment.getFileName(),
                attachment.getFileSize(),
                attachment.getContentType(),
                attachment.getExtension(),
                attachment.getPreviewable() != null && attachment.getPreviewable() == 1,
                attachment.getPreviewType(),
                attachment.getDownloadOnly() != null && attachment.getDownloadOnly() == 1,
                "/api/files/" + attachment.getId() + "/download",
                String.valueOf(attachment.getUploaderId()),
                attachment.getUploaderName(),
                format(attachment.getCreateTime())
        );
    }

    private String format(LocalDateTime time) {
        return time == null ? null : DATE_TIME_FORMATTER.format(time);
    }
}
