package com.opsdesk.ticket.mapper;

import com.opsdesk.ticket.entity.TicketWatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 工单关注关系数据访问 Mapper。
 *
 * <p>用于详情页关注按钮和“我关注”范围查询，保持逻辑删除以保留用户操作痕迹。</p>
 */
@Mapper
public interface TicketWatchMapper {

    int countActive(@Param("ticketId") Long ticketId,
                    @Param("userId") Long userId);

    int insert(TicketWatch watch);

    int restoreDeleted(TicketWatch watch);

    int logicalDelete(@Param("ticketId") Long ticketId,
                      @Param("userId") Long userId,
                      @Param("operatorId") Long operatorId);
}
