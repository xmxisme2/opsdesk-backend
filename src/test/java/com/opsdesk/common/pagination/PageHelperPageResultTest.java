package com.opsdesk.common.pagination;

import com.opsdesk.common.response.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageHelper 分页结果转换测试。
 *
 * <p>验证统一分页工具能够复用 PageQuery 规则，并保持接口层 PageResult 契约不变。</p>
 */
class PageHelperPageResultTest {

    @Test
    void selectPageShouldNormalizeRequestAndConvertRecords() {
        PageQuery request = new PageQuery();
        request.setPage(0L);
        request.setSize(200L);

        PageResult<String> result = PageHelperPageResult.selectPage(
                request,
                () -> List.of(1, 2),
                value -> "role-" + value
        );

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(100);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.records()).containsExactly("role-1", "role-2");
    }
}
