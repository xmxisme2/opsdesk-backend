package com.opsdesk.ticket.service;

import com.opsdesk.ticket.dto.TicketCategoryTreeRequest;
import com.opsdesk.ticket.vo.TicketCategoryVO;

import java.util.List;

/**
 * 工单分类服务。
 *
 * <p>首版提供创建工单和列表筛选所需的分类树读取能力，维护能力后续归入系统配置模块。</p>
 */
public interface TicketCategoryService {

    List<TicketCategoryVO> tree(TicketCategoryTreeRequest request);
}
