package com.opsdesk.ticket.service;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.ticket.mapper.TicketMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 工单编号生成器。
 *
 * <p>编号只在提交工单时生成，草稿允许 ticket_no 为空；格式为 TK + yyyyMMdd + 4 位当日序号。</p>
 */
@Component
public class TicketNoGenerator {

    /** 工单编号业务前缀：用于和其他业务单据区分，外部不允许传入或覆盖。 */
    private static final String TICKET_NO_PREFIX = "TK";

    /** 当日最大序号：首版每天最多生成 9999 个编号，超过后提示状态冲突并由运维扩容规则。 */
    private static final int MAX_DAILY_SEQUENCE = 9999;

    private final TicketMapper ticketMapper;

    public TicketNoGenerator(TicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    public String nextNo() {
        String prefix = TICKET_NO_PREFIX + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int sequence = 1; sequence <= MAX_DAILY_SEQUENCE; sequence++) {
            String ticketNo = prefix + String.format("%04d", sequence);
            if (ticketMapper.countByTicketNo(ticketNo) == 0) {
                return ticketNo;
            }
        }
        throw new BusinessException(ErrorCode.STATE_CONFLICT, "当天工单编号已达到上限，请稍后再试");
    }
}
