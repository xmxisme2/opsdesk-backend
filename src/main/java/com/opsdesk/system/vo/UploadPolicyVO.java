package com.opsdesk.system.vo;

import java.util.List;

/** 上传限制响应，供管理端配置和附件服务运行时校验共同使用。 */
public record UploadPolicyVO(int maxFileSizeMb, int maxFilesPerTicket,
                             List<String> allowedExtensions, List<String> previewableExtensions,
                             List<String> downloadOnlyExtensions) {}
