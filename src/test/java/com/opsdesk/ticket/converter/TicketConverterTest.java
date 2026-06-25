package com.opsdesk.ticket.converter;

import com.opsdesk.attachment.vo.AttachmentVO;
import com.opsdesk.ticket.entity.Ticket;
import com.opsdesk.ticket.vo.TicketVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单转换器测试。
 *
 * <p>附件模块接入后，工单详情必须返回强类型附件列表，避免前端详情页仍拿到空占位。</p>
 */
class TicketConverterTest {

    private final TicketConverter converter = new TicketConverter();

    @Test
    void toVOShouldCarryTypedAttachments() {
        AttachmentVO attachment = new AttachmentVO(
                "500", "TICKET", "100", null, "note.txt", 5L,
                "text/plain", "txt", true, "TEXT", false,
                "/api/files/500/download", "10", "张三", "2026-06-18 10:00:00"
        );

        TicketVO result = converter.toVO(ticket(), null, null, null, null, false, List.of(attachment));

        assertThat(result.attachments()).containsExactly(attachment);
    }

    private Ticket ticket() {
        Ticket ticket = new Ticket();
        ticket.setId(100L);
        ticket.setTitle("无法登录");
        ticket.setDescription("登录失败");
        ticket.setCategoryId(1L);
        ticket.setPriority("MEDIUM");
        ticket.setStatus("DRAFT");
        ticket.setCreatorId(10L);
        ticket.setOverdue(0);
        ticket.setCreateTime(LocalDateTime.of(2026, 6, 18, 10, 0));
        ticket.setUpdateTime(LocalDateTime.of(2026, 6, 18, 10, 0));
        return ticket;
    }
}
