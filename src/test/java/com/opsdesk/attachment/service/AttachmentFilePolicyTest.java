package com.opsdesk.attachment.service;

import com.opsdesk.attachment.model.ValidatedAttachmentFile;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 附件文件策略测试。
 *
 * <p>覆盖扩展名、MIME、大小、文件名安全和预览分类，防止危险文件进入本地存储。</p>
 */
class AttachmentFilePolicyTest {

    private final AttachmentFilePolicy filePolicy = new AttachmentFilePolicy();

    @Test
    void validateShouldClassifyPngAsImagePreview() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "screen.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        ValidatedAttachmentFile result = filePolicy.validate(file);

        assertThat(result.extension()).isEqualTo("png");
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.previewType()).isEqualTo("IMAGE");
        assertThat(result.previewable()).isTrue();
        assertThat(result.downloadOnly()).isFalse();
    }

    @Test
    void validateShouldRejectMimeMismatch() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "screen.png",
                "application/pdf",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        assertThatThrownBy(() -> filePolicy.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }

    @Test
    void validateShouldRejectPngWithInvalidSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "screen.png",
                "image/png",
                "not-a-real-png".getBytes()
        );

        assertThatThrownBy(() -> filePolicy.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }

    @Test
    void validateShouldRejectUnsafeFileName() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../screen.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        assertThatThrownBy(() -> filePolicy.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }

    @Test
    void validateShouldRejectOrdinaryZipDisguisedAsDocx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                zipBytes("readme.txt")
        );

        assertThatThrownBy(() -> filePolicy.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }

    @Test
    void validateShouldRejectFileLargerThanTwentyMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.txt",
                "text/plain",
                new byte[20 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> filePolicy.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }

    private byte[] zipBytes(String entryName) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write("content".getBytes());
            zipOutputStream.closeEntry();
        }
        return outputStream.toByteArray();
    }
}
