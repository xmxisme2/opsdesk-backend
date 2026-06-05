package com.opsdesk.common.util;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 接口层字符串 ID 解析工具。
 *
 * <p>前端统一按字符串传输 BIGINT ID，本工具集中完成 Long 转换、正数校验和去重，避免各模块重复散落解析逻辑。</p>
 */
public final class IdParser {

    private IdParser() {
    }

    public static Long parseRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "不能为空");
        }
        try {
            Long id = Long.valueOf(value.trim());
            if (id <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "必须为正整数");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + "格式不正确");
        }
    }

    public static List<Long> parseDistinctList(List<String> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (String value : values) {
            ids.add(parseRequired(value, fieldName));
        }
        return new ArrayList<>(ids);
    }
}
