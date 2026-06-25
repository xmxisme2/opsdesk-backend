-- OpsDesk 本地库迁移脚本：补齐临时附件关联字段。
-- 背景：上传接口允许 bizId 为空，并通过 tempToken 关联创建业务前的临时附件。
-- 执行方式：请使用 mysql 客户端 source 方式执行，避免 PowerShell 管道导致中文 SQL 乱码。

SET NAMES utf8mb4;
USE opsdesk;

ALTER TABLE ticket_attachment
  MODIFY biz_id BIGINT NULL COMMENT '业务 ID，临时附件为空';

SET @temp_token_column_count = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ticket_attachment'
    AND COLUMN_NAME = 'temp_token'
);
SET @add_temp_token_sql = IF(
  @temp_token_column_count = 0,
  'ALTER TABLE ticket_attachment ADD COLUMN temp_token VARCHAR(64) NULL COMMENT ''临时附件令牌，绑定业务后清空'' AFTER biz_id',
  'SELECT 1'
);
PREPARE add_temp_token_statement FROM @add_temp_token_sql;
EXECUTE add_temp_token_statement;
DEALLOCATE PREPARE add_temp_token_statement;

SET @temp_token_index_count = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ticket_attachment'
    AND INDEX_NAME = 'idx_ticket_attachment_temp'
);
SET @add_temp_token_index_sql = IF(
  @temp_token_index_count = 0,
  'ALTER TABLE ticket_attachment ADD INDEX idx_ticket_attachment_temp (temp_token, uploader_id, deleted)',
  'SELECT 1'
);
PREPARE add_temp_token_index_statement FROM @add_temp_token_index_sql;
EXECUTE add_temp_token_index_statement;
DEALLOCATE PREPARE add_temp_token_index_statement;
