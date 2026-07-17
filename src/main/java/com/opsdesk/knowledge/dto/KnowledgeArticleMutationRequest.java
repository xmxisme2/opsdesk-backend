package com.opsdesk.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 知识库文章创建和编辑请求。 */
@Getter
@Setter
public class KnowledgeArticleMutationRequest {
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题不能超过200个字符")
    private String title;

    @Size(max = 500, message = "文章摘要不能超过500个字符")
    private String summary;

    @NotBlank(message = "文章正文不能为空")
    private String content;

    private String categoryId;
    private List<@Size(max = 64, message = "标签不能超过64个字符") String> tags;
    private String sourceTicketId;
    private String status;
}
