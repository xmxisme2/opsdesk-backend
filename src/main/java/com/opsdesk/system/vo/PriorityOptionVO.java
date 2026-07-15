package com.opsdesk.system.vo;

/**
 * 工单优先级选项。
 *
 * @param code 固定优先级编码，不允许外部扩展或修改
 * @param name 前端展示名称
 * @param sort 展示顺序，四项之间必须唯一
 * @param color 六位十六进制展示颜色
 * @param enabled 是否允许新工单选择
 */
public record PriorityOptionVO(String code, String name, Integer sort, String color, Boolean enabled) {
}
