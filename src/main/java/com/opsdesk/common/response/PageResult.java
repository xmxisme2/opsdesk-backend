package com.opsdesk.common.response;

import java.util.List;

/**
 * 统一分页响应数据结构。
 *
 * <p>列表接口统一返回 records、page、size、total，方便前端表格组件复用。</p>
 */
public record PageResult<T>(
        List<T> records,
        long page,
        long size,
        long total
) {

    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(List.of(), page, size, 0);
    }
}

