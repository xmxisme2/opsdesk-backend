package com.opsdesk.ticket.mapper;

import com.opsdesk.ticket.entity.TicketCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单分类数据访问 Mapper。
 *
 * <p>提供分类树读取和分类维护所需的数据访问，不承载树循环、团队状态等业务判断。</p>
 */
@Mapper
public interface TicketCategoryMapper {

    TicketCategory findById(@Param("id") Long id);

    List<TicketCategory> searchTree(@Param("enabled") Integer enabled,
                                    @Param("keyword") String keyword);

    int countByParentAndName(@Param("parentId") Long parentId,
                             @Param("name") String name,
                             @Param("excludeId") Long excludeId);

    int countDescendantRelation(@Param("categoryId") Long categoryId,
                                @Param("candidateParentId") Long candidateParentId);

    int countChildren(@Param("parentId") Long parentId);

    int countTickets(@Param("categoryId") Long categoryId);

    int insert(TicketCategory category);

    int update(TicketCategory category);

    int logicalDelete(@Param("id") Long id,
                      @Param("operatorId") Long operatorId);
}
