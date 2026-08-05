package com.opsdesk.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户提交 AI 回答反馈的请求参数。 */
public record AiFeedbackRequest(@NotBlank String rating, String reasonCode, @Size(max = 1000) String comment) { }
