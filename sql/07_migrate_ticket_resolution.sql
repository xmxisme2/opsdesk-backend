-- 为既有本地库补齐工单结构化解决方案字段。
-- 每条语句独立判断，确保重复执行不会失败。
SET @schema_name = DATABASE();

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'ticket' AND column_name = 'resolution_summary'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ticket ADD COLUMN resolution_summary VARCHAR(1000) NULL COMMENT ''解决方案摘要或根因结论'' AFTER description',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'ticket' AND column_name = 'resolution_steps'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ticket ADD COLUMN resolution_steps TEXT NULL COMMENT ''可复用的处理步骤，支持 Markdown'' AFTER resolution_summary',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = @schema_name AND table_name = 'ticket' AND column_name = 'resolution_verified'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ticket ADD COLUMN resolution_verified TINYINT NOT NULL DEFAULT 0 COMMENT ''解决结果是否已验证：0 否，1 是'' AFTER resolution_steps',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
