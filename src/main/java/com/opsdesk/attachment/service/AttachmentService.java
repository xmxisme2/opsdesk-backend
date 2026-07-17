package com.opsdesk.attachment.service;

import com.opsdesk.attachment.dto.AttachmentDeleteRequest;
import com.opsdesk.attachment.dto.AttachmentSearchRequest;
import com.opsdesk.attachment.dto.AttachmentUploadRequest;
import com.opsdesk.attachment.model.AttachmentDownload;
import com.opsdesk.attachment.model.AttachmentPreviewResult;
import com.opsdesk.attachment.vo.AttachmentVO;
import com.opsdesk.common.security.CurrentUser;

import java.util.List;

/**
 * 附件业务服务。
 *
 * <p>负责上传编排、资源范围、数量限制、下载预览和逻辑删除。</p>
 */
public interface AttachmentService {

    AttachmentVO upload(AttachmentUploadRequest request,
                        CurrentUser currentUser,
                        String requestIp,
                        String userAgent);

    List<AttachmentVO> search(AttachmentSearchRequest request, CurrentUser currentUser);

    /**
     * 将当前上传人的指定临时附件绑定到已创建业务资源。
     *
     * <p>调用方只可提交本次创建流程产生的附件 ID；服务会再次校验业务写权限和附件归属。</p>
     */
    List<AttachmentVO> bindTemporaryAttachments(String bizType,
                                                Long bizId,
                                                List<String> attachmentIds,
                                                CurrentUser currentUser);

    /** 业务资源删除前同步逻辑删除已绑定附件，避免保留无法访问的元数据。 */
    int logicalDeleteBoundAttachments(String bizType, Long bizId, CurrentUser currentUser);

    AttachmentDownload download(String id, CurrentUser currentUser);

    AttachmentPreviewResult preview(String id, CurrentUser currentUser);

    void delete(String id,
                AttachmentDeleteRequest request,
                CurrentUser currentUser,
                String requestIp,
                String userAgent);
}
