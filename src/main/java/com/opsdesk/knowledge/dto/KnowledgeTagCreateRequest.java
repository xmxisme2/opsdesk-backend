package com.opsdesk.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 知识库标签创建请求。 */
@Getter
@Setter
public class KnowledgeTagCreateRequest {
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 64, message = "标签名称不能超过64个字符")
    private String name;
}
