package com.opsdesk.common.pagination;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一分页查询请求。
 *
 * <p>列表接口优先继承本类复用 page、size、排序字段和排序方向，避免每个模块重复定义语义相同的分页实体。</p>
 */
@Getter
@Setter
public class PageQuery {

    /** 默认页码：前端未传或传入非法页码时从第 1 页开始，不允许外部传入小于 1 的值生效。 */
    private static final long DEFAULT_PAGE = 1L;

    /** 默认每页条数：前端未传或传入非法条数时使用 20 条，适合后台表格默认密度。 */
    private static final long DEFAULT_SIZE = 20L;

    /** 最大每页条数：限制后台列表单次查询上限，避免外部传入过大分页拖慢数据库。 */
    private static final long MAX_SIZE = 100L;

    private Long page = DEFAULT_PAGE;
    private Long size = DEFAULT_SIZE;
    private String sortBy;
    private String sortOrder;

    public long normalizedPage() {
        return page == null || page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
    }

    public long normalizedSize() {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
