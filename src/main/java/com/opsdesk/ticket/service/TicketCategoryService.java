package com.opsdesk.ticket.service;

import com.opsdesk.ticket.dto.TicketCategoryMutationRequest;
import com.opsdesk.ticket.dto.TicketCategoryTreeRequest;
import com.opsdesk.ticket.vo.TicketCategoryVO;

import java.util.List;

/**
 * 工单分类服务。
 *
 * <p>提供分类树读取和管理员维护能力，写操作统一在服务层完成树结构、团队和删除保护校验。</p>
 */
public interface TicketCategoryService {

    List<TicketCategoryVO> tree(TicketCategoryTreeRequest request);

    /** 创建工单分类并记录配置审计日志。 */
    TicketCategoryVO create(TicketCategoryMutationRequest request,
                            Long operatorId,
                            String requestIp,
                            String userAgent);

    /** 编辑工单分类，禁止把自身或后代设置为父级。 */
    TicketCategoryVO update(String id,
                            TicketCategoryMutationRequest request,
                            Long operatorId,
                            String requestIp,
                            String userAgent);

    /** 在无子分类且无关联工单时逻辑删除分类。 */
    void delete(String id, Long operatorId, String requestIp, String userAgent);
}
