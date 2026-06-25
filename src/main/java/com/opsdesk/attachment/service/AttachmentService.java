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

    AttachmentDownload download(String id, CurrentUser currentUser);

    AttachmentPreviewResult preview(String id, CurrentUser currentUser);

    void delete(String id,
                AttachmentDeleteRequest request,
                CurrentUser currentUser,
                String requestIp,
                String userAgent);
}
