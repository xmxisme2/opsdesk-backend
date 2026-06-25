package com.opsdesk.ticket.service;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.ticket.enums.TicketAction;
import com.opsdesk.ticket.enums.TicketStatus;
import org.springframework.stereotype.Component;

/**
 * 工单状态机。
 *
 * <p>集中维护工单主状态流转和动作权限，Controller 与业务 Service 禁止绕过本类直接修改工单状态。</p>
 */
@Component
public class TicketStateMachine {

    public TicketStatus nextStatus(TicketStatus currentStatus, TicketAction action, TicketStateContext context) {
        if (currentStatus == null || action == null || context == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工单状态动作参数不完整");
        }

        TicketStatus targetStatus = resolveTargetStatus(currentStatus, action);
        validateOperator(currentStatus, action, context);
        return targetStatus;
    }

    private TicketStatus resolveTargetStatus(TicketStatus currentStatus, TicketAction action) {
        return switch (action) {
            case SUBMIT -> requireStatus(currentStatus, TicketStatus.DRAFT, TicketStatus.PENDING_ASSIGN, action);
            case CANCEL -> requireAnyStatus(currentStatus, action, TicketStatus.DRAFT, TicketStatus.PENDING_ASSIGN)
                    ? TicketStatus.CANCELLED
                    : throwStateConflict(currentStatus, action);
            case ASSIGN -> requireStatus(currentStatus, TicketStatus.PENDING_ASSIGN, TicketStatus.PENDING_PROCESS, action);
            case ACCEPT -> requireStatus(currentStatus, TicketStatus.PENDING_PROCESS, TicketStatus.PROCESSING, action);
            case TRANSFER -> requireAnyStatus(currentStatus, action, TicketStatus.PENDING_PROCESS, TicketStatus.PROCESSING)
                    ? TicketStatus.PENDING_PROCESS
                    : throwStateConflict(currentStatus, action);
            case REJECT -> requireAnyStatus(currentStatus, action, TicketStatus.PENDING_ASSIGN, TicketStatus.PROCESSING)
                    ? TicketStatus.DRAFT
                    : throwStateConflict(currentStatus, action);
            case COMPLETE -> requireStatus(currentStatus, TicketStatus.PROCESSING, TicketStatus.PENDING_CONFIRM, action);
            case CONFIRM -> requireStatus(currentStatus, TicketStatus.PENDING_CONFIRM, TicketStatus.COMPLETED, action);
            case REOPEN -> requireStatus(currentStatus, TicketStatus.PENDING_CONFIRM, TicketStatus.PROCESSING, action);
            case CLOSE -> requireStatus(currentStatus, TicketStatus.COMPLETED, TicketStatus.CLOSED, action);
        };
    }

    private void validateOperator(TicketStatus currentStatus, TicketAction action, TicketStateContext context) {
        boolean allowed = switch (action) {
            case SUBMIT, CONFIRM, REOPEN -> context.isCreator();
            case CANCEL -> context.isCreator() || (currentStatus == TicketStatus.PENDING_ASSIGN && context.isAdmin());
            case ASSIGN -> context.isManagerOrAdmin();
            case ACCEPT, TRANSFER -> context.isAgentOrAbove()
                    && (context.isCurrentAssignee() || context.isTeamMember() || context.isManagerOrAdmin());
            case REJECT -> currentStatus == TicketStatus.PENDING_ASSIGN
                    ? context.isManagerOrAdmin()
                    : context.isCurrentAssignee();
            case COMPLETE -> context.isCurrentAssignee();
            case CLOSE -> context.isCreator() || context.isManagerOrAdmin();
        };

        if (!allowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无权执行该工单动作");
        }
    }

    private TicketStatus requireStatus(TicketStatus currentStatus,
                                       TicketStatus expectedStatus,
                                       TicketStatus targetStatus,
                                       TicketAction action) {
        if (currentStatus != expectedStatus) {
            return throwStateConflict(currentStatus, action);
        }
        return targetStatus;
    }

    private boolean requireAnyStatus(TicketStatus currentStatus, TicketAction action, TicketStatus... expectedStatuses) {
        for (TicketStatus expectedStatus : expectedStatuses) {
            if (currentStatus == expectedStatus) {
                return true;
            }
        }
        return false;
    }

    private <T> T throwStateConflict(TicketStatus currentStatus, TicketAction action) {
        throw new BusinessException(ErrorCode.STATE_CONFLICT, "工单状态 " + currentStatus + " 不允许执行动作 " + action);
    }
}
