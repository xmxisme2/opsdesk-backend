package com.opsdesk.ticket.service;

import com.opsdesk.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 工单编号生成器单元测试。
 *
 * <p>覆盖提交时生成编号的核心规则，确保当天已有编号时继续递增，避免与数据库唯一约束冲突。</p>
 */
class TicketNoGeneratorTest {

    @Test
    void nextNoShouldSkipExistingSequence() {
        TicketMapper ticketMapper = mock(TicketMapper.class);
        TicketNoGenerator generator = new TicketNoGenerator(ticketMapper);
        String prefix = "TK" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        when(ticketMapper.countByTicketNo(prefix + "0001")).thenReturn(1);
        when(ticketMapper.countByTicketNo(prefix + "0002")).thenReturn(0);

        assertThat(generator.nextNo()).isEqualTo(prefix + "0002");
    }
}
