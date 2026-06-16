package com.opsdesk.ticket.mapper;

import com.opsdesk.ticket.entity.TicketCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工单分类数据访问 Mapper。
 *
 * <p>当前首版提供分类树和分类详情读取，分类维护接口后续在系统配置模块补齐。</p>
 */
@Mapper
public interface TicketCategoryMapper {

    @Select("""
            SELECT *
            FROM ticket_category
            WHERE id = #{id}
              AND deleted = 0
            LIMIT 1
            """)
    TicketCategory findById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT *
            FROM ticket_category
            WHERE deleted = 0
            <if test="enabled != null">
              AND enabled = #{enabled}
            </if>
            <if test="keyword != null and keyword != ''">
              AND name LIKE CONCAT('%', #{keyword}, '%')
            </if>
            ORDER BY parent_id ASC, sort ASC, id ASC
            </script>
            """)
    List<TicketCategory> searchTree(@Param("enabled") Integer enabled,
                                    @Param("keyword") String keyword);
}
