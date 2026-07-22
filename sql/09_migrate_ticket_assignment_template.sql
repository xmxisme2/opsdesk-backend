-- 工单分派通知统一展示处理组，避免仅分派团队时将未指定处理人渲染为“待分派”。
UPDATE notification_template
SET content_template = '{operatorName} 将工单 {ticketNo} 分派给 {teamName}，请及时处理。',
    update_time = CURRENT_TIMESTAMP
WHERE type = 'TICKET_ASSIGNED'
  AND channel = 'IN_APP'
  AND deleted = 0;
