package com.opsdesk.ticket.mapper;

import com.opsdesk.ticket.entity.TicketCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单分类数据访问 Mapper。
 *
 * <p>当前首版提供分类树和分类详情读取，分类维护接口后续在系统配置模块补齐。</p>
 */
@Mapper
public interface TicketCategoryMapper {

    TicketCategory findById(@Param("id") Long id);

    List<TicketCategory> searchTree(@Param("enabled") Integer enabled,
                                    @Param("keyword") String keyword);
}
