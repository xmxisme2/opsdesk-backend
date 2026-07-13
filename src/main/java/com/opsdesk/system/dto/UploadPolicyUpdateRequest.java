package com.opsdesk.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 上传限制更新请求，扩展名最终仍受后端文件安全能力约束。 */
public record UploadPolicyUpdateRequest(
        @Min(1) @Max(100) int maxFileSizeMb,
        @Min(1) @Max(50) int maxFilesPerTicket,
        @NotEmpty List<String> allowedExtensions,
        List<String> previewableExtensions,
        List<String> downloadOnlyExtensions
) {}
