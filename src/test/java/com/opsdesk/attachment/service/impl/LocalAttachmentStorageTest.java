package com.opsdesk.attachment.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 本地附件存储测试。
 *
 * <p>验证返回值始终为相对路径，并阻止读取存储根目录之外的文件。</p>
 */
class LocalAttachmentStorageTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void storeShouldReturnRelativePathAndLoadContent() throws Exception {
        LocalAttachmentStorage storage = new LocalAttachmentStorage(tempDirectory);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        String storagePath = storage.store(file, "txt", 100L);
        Resource resource = storage.load(storagePath);

        assertThat(Path.of(storagePath)).isRelative();
        assertThat(storagePath).doesNotContain(tempDirectory.toString());
        assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void loadShouldRejectPathOutsideStorageRoot() {
        LocalAttachmentStorage storage = new LocalAttachmentStorage(tempDirectory);

        assertThatThrownBy(() -> storage.load("../secret.txt"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
