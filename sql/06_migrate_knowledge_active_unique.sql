-- 知识库活动数据唯一约束迁移：逻辑删除历史允许重复，仅 deleted=0 的数据参与唯一校验。
SET NAMES utf8mb4;
USE opsdesk;

DROP PROCEDURE IF EXISTS migrate_knowledge_active_unique;
DELIMITER $$
CREATE PROCEDURE migrate_knowledge_active_unique()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='knowledge_category' AND column_name='active_parent_id') THEN
    ALTER TABLE knowledge_category ADD COLUMN active_parent_id BIGINT GENERATED ALWAYS AS (CASE WHEN deleted=0 THEN COALESCE(parent_id,0) ELSE NULL END) STORED COMMENT '活动分类父级唯一键';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='knowledge_category' AND index_name='uk_knowledge_category_name_deleted') THEN
    ALTER TABLE knowledge_category DROP INDEX uk_knowledge_category_name_deleted;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='knowledge_category' AND index_name='uk_knowledge_category_active_name') THEN
    ALTER TABLE knowledge_category ADD UNIQUE KEY uk_knowledge_category_active_name(active_parent_id,name);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='knowledge_tag' AND column_name='active_name') THEN
    ALTER TABLE knowledge_tag ADD COLUMN active_name VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN deleted=0 THEN name ELSE NULL END) STORED COMMENT '活动标签唯一键';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='knowledge_tag' AND index_name='uk_knowledge_tag_name_deleted') THEN
    ALTER TABLE knowledge_tag DROP INDEX uk_knowledge_tag_name_deleted;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='knowledge_tag' AND index_name='uk_knowledge_tag_active_name') THEN
    ALTER TABLE knowledge_tag ADD UNIQUE KEY uk_knowledge_tag_active_name(active_name);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='knowledge_article_tag' AND column_name='active_tag_id') THEN
    ALTER TABLE knowledge_article_tag ADD COLUMN active_tag_id BIGINT GENERATED ALWAYS AS (CASE WHEN deleted=0 THEN tag_id ELSE NULL END) STORED COMMENT '活动文章标签唯一键';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='knowledge_article_tag' AND index_name='uk_knowledge_article_tag_deleted') THEN
    ALTER TABLE knowledge_article_tag DROP INDEX uk_knowledge_article_tag_deleted;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='knowledge_article_tag' AND index_name='uk_knowledge_article_tag_active') THEN
    ALTER TABLE knowledge_article_tag ADD UNIQUE KEY uk_knowledge_article_tag_active(article_id,active_tag_id);
  END IF;
END$$
DELIMITER ;
CALL migrate_knowledge_active_unique();
DROP PROCEDURE migrate_knowledge_active_unique;
