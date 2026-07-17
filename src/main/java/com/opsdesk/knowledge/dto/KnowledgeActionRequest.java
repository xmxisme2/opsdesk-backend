package com.opsdesk.knowledge.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 发布、下线和删除文章时的可选备注。 */
@Getter
@Setter
public class KnowledgeActionRequest {
    @Size(max = 500, message = "备注不能超过500个字符")
    private String reason;
    @Size(max = 500, message = "发布备注不能超过500个字符")
    private String publishRemark;
}
