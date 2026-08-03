-- 为知识文章增加业务版本，并创建事务消息 Outbox。
USE opsdesk;

SET @schema_name = DATABASE();
SET @column_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'knowledge_article'
    AND column_name = 'version'
);
SET @sql = IF(
  @column_exists = 0,
  'ALTER TABLE knowledge_article ADD COLUMN version BIGINT NOT NULL DEFAULT 1 COMMENT ''文章业务版本，每次内容或状态变化递增'' AFTER author_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS event_outbox (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  event_id VARCHAR(64) NOT NULL COMMENT '全局事件 ID',
  aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合类型',
  aggregate_id BIGINT NOT NULL COMMENT '聚合 ID',
  event_type VARCHAR(128) NOT NULL COMMENT '事件类型',
  event_version VARCHAR(16) NOT NULL COMMENT '事件协议版本',
  routing_key VARCHAR(128) NOT NULL COMMENT 'RabbitMQ routing key',
  payload LONGTEXT NOT NULL COMMENT 'JSON 事件信封',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/PUBLISHED/FAILED',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '发布重试次数',
  next_retry_time DATETIME NULL COMMENT '下次重试时间',
  published_time DATETIME NULL COMMENT '成功发布时间',
  last_error VARCHAR(1000) NULL COMMENT '最近脱敏错误摘要',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_event_outbox_event_id (event_id),
  KEY idx_event_outbox_dispatch (status, next_retry_time, create_time),
  KEY idx_event_outbox_aggregate (aggregate_type, aggregate_id, create_time)
) COMMENT='事务消息 Outbox';
