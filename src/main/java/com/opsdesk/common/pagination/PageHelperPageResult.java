package com.opsdesk.common.pagination;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.opsdesk.common.response.PageResult;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * PageHelper 分页结果工具。
 *
 * <p>统一把 PageQuery 规范化后的分页参数交给 PageHelper，并把 PageInfo 转回接口层固定的 PageResult，
 * 避免各业务 Service 重复手写 total、offset 和空分页分支。</p>
 */
public final class PageHelperPageResult {

    private PageHelperPageResult() {
    }

    public static <T> PageResult<T> selectPage(PageQuery request, Supplier<List<T>> selectSupplier) {
        return selectPage(request, selectSupplier, Function.identity());
    }

    public static <T, R> PageResult<R> selectPage(PageQuery request,
                                                  Supplier<List<T>> selectSupplier,
                                                  Function<T, R> converter) {
        PageQuery safeRequest = request == null ? new PageQuery() : request;
        long page = safeRequest.normalizedPage();
        long size = safeRequest.normalizedSize();
        PageHelper.startPage((int) page, (int) size);
        try {
            List<T> sourceRecords = selectSupplier.get();
            PageInfo<T> pageInfo = new PageInfo<>(sourceRecords);
            List<R> records = sourceRecords.stream()
                    .map(converter)
                    .toList();
            return new PageResult<>(records, page, size, pageInfo.getTotal());
        } finally {
            PageHelper.clearPage();
        }
    }
}
