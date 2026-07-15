-- OpsDesk 本地库迁移脚本：修复工单分类同级活动名称唯一约束。
-- 目标：根分类使用 COALESCE(parent_id, 0) 参与唯一约束，已删除历史不再阻塞同名分类反复创建和删除。
-- 执行方式：请使用 mysql 客户端 source；脚本可重复执行。

SET NAMES utf8mb4;
USE opsdesk;

SET @old_index_count = (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ticket_category'
    AND INDEX_NAME = 'uk_ticket_category_name_deleted'
);
SET @drop_old_index_sql = IF(
  @old_index_count > 0,
  'ALTER TABLE ticket_category DROP INDEX uk_ticket_category_name_deleted',
  'SELECT 1'
);
PREPARE drop_old_index_statement FROM @drop_old_index_sql;
EXECUTE drop_old_index_statement;
DEALLOCATE PREPARE drop_old_index_statement;

SET @active_parent_column_count = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ticket_category'
    AND COLUMN_NAME = 'active_parent_id'
);
SET @add_active_parent_sql = IF(
  @active_parent_column_count = 0,
  'ALTER TABLE ticket_category ADD COLUMN active_parent_id BIGINT GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN COALESCE(parent_id, 0) ELSE NULL END) STORED COMMENT ''活动分类父级唯一键，根分类统一映射为 0'' AFTER deleted',
  'SELECT 1'
);
PREPARE add_active_parent_statement FROM @add_active_parent_sql;
EXECUTE add_active_parent_statement;
DEALLOCATE PREPARE add_active_parent_statement;

SET @active_name_column_count = (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ticket_category'
    AND COLUMN_NAME = 'active_name'
);
SET @add_active_name_sql = IF(
  @active_name_column_count = 0,
  'ALTER TABLE ticket_category ADD COLUMN active_name VARCHAR(128) GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN name ELSE NULL END) STORED COMMENT ''活动分类名称唯一键，已删除记录为空以允许重复历史'' AFTER active_parent_id',
  'SELECT 1'
);
PREPARE add_active_name_statement FROM @add_active_name_sql;
EXECUTE add_active_name_statement;
DEALLOCATE PREPARE add_active_name_statement;

SET @active_unique_index_count = (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ticket_category'
    AND INDEX_NAME = 'uk_ticket_category_active_name'
);
SET @add_active_unique_index_sql = IF(
  @active_unique_index_count = 0,
  'ALTER TABLE ticket_category ADD UNIQUE INDEX uk_ticket_category_active_name (active_parent_id, active_name)',
  'SELECT 1'
);
PREPARE add_active_unique_index_statement FROM @add_active_unique_index_sql;
EXECUTE add_active_unique_index_statement;
DEALLOCATE PREPARE add_active_unique_index_statement;
