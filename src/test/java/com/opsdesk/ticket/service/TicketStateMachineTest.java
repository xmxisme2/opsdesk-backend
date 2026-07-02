package com.opsdesk.ticket.service;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.ticket.enums.TicketAction;
import com.opsdesk.ticket.enums.TicketStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 工单状态机单元测试。
 *
 * <p>先覆盖 MVP 主流程和关键越权场景，保证后续 Controller 和 Service 只能通过状态机完成流转。</p>
 */
class TicketStateMachineTest {

    private final TicketStateMachine stateMachine = new TicketStateMachine();

    @Test
    void mainFlowShouldMoveFromDraftToClosed() {
        TicketStatus status = stateMachine.nextStatus(TicketStatus.DRAFT, TicketAction.SUBMIT, creator());
        assertThat(status).isEqualTo(TicketStatus.PENDING_ASSIGN);

        status = stateMachine.nextStatus(status, TicketAction.ASSIGN, manager());
        assertThat(status).isEqualTo(TicketStatus.PENDING_PROCESS);

        status = stateMachine.nextStatus(status, TicketAction.ACCEPT, assignee());
        assertThat(status).isEqualTo(TicketStatus.PROCESSING);

        status = stateMachine.nextStatus(status, TicketAction.COMPLETE, assignee());
        assertThat(status).isEqualTo(TicketStatus.PENDING_CONFIRM);

        status = stateMachine.nextStatus(status, TicketAction.CONFIRM, creator());
        assertThat(status).isEqualTo(TicketStatus.COMPLETED);

        status = stateMachine.nextStatus(status, TicketAction.CLOSE, creator());
        assertThat(status).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    void cancelShouldOnlyAllowDraftAndPendingAssign() {
        assertThat(stateMachine.nextStatus(TicketStatus.DRAFT, TicketAction.CANCEL, creator()))
                .isEqualTo(TicketStatus.CANCELLED);
        assertThat(stateMachine.nextStatus(TicketStatus.PENDING_ASSIGN, TicketAction.CANCEL, creator()))
                .isEqualTo(TicketStatus.CANCELLED);

        assertStateConflict(TicketStatus.PROCESSING, TicketAction.CANCEL, assignee());
    }

    @Test
    void rejectShouldReturnToDraftOnlyFromPendingAssignOrProcessing() {
        assertThat(stateMachine.nextStatus(TicketStatus.PENDING_ASSIGN, TicketAction.REJECT, manager()))
                .isEqualTo(TicketStatus.DRAFT);
        assertThat(stateMachine.nextStatus(TicketStatus.PROCESSING, TicketAction.REJECT, assignee()))
                .isEqualTo(TicketStatus.DRAFT);

        assertStateConflict(TicketStatus.PENDING_CONFIRM, TicketAction.REJECT, assignee());
    }

    @Test
    void transferShouldKeepTicketPendingProcess() {
        assertThat(stateMachine.nextStatus(TicketStatus.PENDING_PROCESS, TicketAction.TRANSFER, teamMember()))
                .isEqualTo(TicketStatus.PENDING_PROCESS);
        assertThat(stateMachine.nextStatus(TicketStatus.PROCESSING, TicketAction.TRANSFER, assignee()))
                .isEqualTo(TicketStatus.PENDING_PROCESS);
    }

    @Test
    void assignedUserShouldAcceptAndTransferWithoutAgentRole() {
        assertThat(stateMachine.nextStatus(TicketStatus.PENDING_PROCESS, TicketAction.ACCEPT, assignedUser()))
                .isEqualTo(TicketStatus.PROCESSING);
        assertThat(stateMachine.nextStatus(TicketStatus.PENDING_PROCESS, TicketAction.TRANSFER, assignedUser()))
                .isEqualTo(TicketStatus.PENDING_PROCESS);
    }

    @Test
    void reopenShouldOnlyAllowPendingConfirm() {
        assertThat(stateMachine.nextStatus(TicketStatus.PENDING_CONFIRM, TicketAction.REOPEN, creator()))
                .isEqualTo(TicketStatus.PROCESSING);

        assertStateConflict(TicketStatus.COMPLETED, TicketAction.REOPEN, creator());
        assertStateConflict(TicketStatus.CLOSED, TicketAction.REOPEN, creator());
    }

    @Test
    void userShouldNotAssignTicket() {
        assertThatThrownBy(() -> stateMachine.nextStatus(TicketStatus.PENDING_ASSIGN, TicketAction.ASSIGN, creator()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void nonAssigneeShouldNotCompleteTicket() {
        assertThatThrownBy(() -> stateMachine.nextStatus(TicketStatus.PROCESSING, TicketAction.COMPLETE, teamMember()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private void assertStateConflict(TicketStatus status, TicketAction action, TicketStateContext context) {
        assertThatThrownBy(() -> stateMachine.nextStatus(status, action, context))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STATE_CONFLICT);
    }

    private TicketStateContext creator() {
        return TicketStateContext.of(10L, 10L, 20L, Set.of("USER"), false);
    }

    private TicketStateContext manager() {
        return TicketStateContext.of(30L, 10L, 20L, Set.of("MANAGER"), true);
    }

    private TicketStateContext assignee() {
        return TicketStateContext.of(20L, 10L, 20L, Set.of("AGENT"), true);
    }

    private TicketStateContext assignedUser() {
        return TicketStateContext.of(20L, 10L, 20L, Set.of("USER"), false);
    }

    private TicketStateContext teamMember() {
        return TicketStateContext.of(21L, 10L, 20L, Set.of("AGENT"), true);
    }
}
