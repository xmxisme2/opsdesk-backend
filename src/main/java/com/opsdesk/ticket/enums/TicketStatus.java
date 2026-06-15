package com.opsdesk.ticket.enums;

/**
 * 工单主状态枚举。
 *
 * <p>状态编码可作为筛选条件从外部传入；状态变更结果只能由后端状态机产生，禁止接口层直接写入。</p>
 */
public enum TicketStatus {

    /** 草稿：创建后未提交，创建人可编辑、提交或取消。 */
    DRAFT,

    /** 待分派：已提交，等待团队负责人或管理员分派。 */
    PENDING_ASSIGN,

    /** 待处理：已分派，等待处理人接单。 */
    PENDING_PROCESS,

    /** 处理中：处理人正在处理。 */
    PROCESSING,

    /** 待确认：处理人已提交完成，等待创建人确认。 */
    PENDING_CONFIRM,

    /** 已完成：创建人已确认，可继续关闭归档。 */
    COMPLETED,

    /** 已关闭：工单归档后的终态，不允许继续流转。 */
    CLOSED,

    /** 已取消：草稿或待分派工单被取消后的终态。 */
    CANCELLED
}
