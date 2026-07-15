package com.opsdesk.system.dto;

import com.opsdesk.system.vo.PriorityOptionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 优先级整体更新请求，必须一次提交四个固定编码，避免产生部分配置状态。 */
public record PriorityConfigUpdateRequest(
        @NotNull @Size(min = 4, max = 4) List<@Valid PriorityOptionVO> items
) {
}
