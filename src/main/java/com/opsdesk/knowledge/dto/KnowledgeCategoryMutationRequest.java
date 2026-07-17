package com.opsdesk.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 知识库分类创建和编辑请求。 */
@Getter
@Setter
public class KnowledgeCategoryMutationRequest {
    private String parentId;
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 128, message = "分类名称不能超过128个字符")
    private String name;
    private Integer sort = 0;
    private Boolean enabled = Boolean.TRUE;
}
