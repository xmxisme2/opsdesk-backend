-- OpsDesk 数据库建表脚本
-- 执行目标：创建本地开发数据库和 V1.0 基础业务表。
-- 注意：不要使用 PowerShell 的 Get-Content ... | mysql 管道执行本文件，管道可能把中文注释或字面量转成问号。
-- 推荐使用 mysql 客户端 source 方式，或执行同目录下的 init-local-db.ps1。

CREATE DATABASE IF NOT EXISTS opsdesk
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE opsdesk;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  phone VARCHAR(32) NOT NULL COMMENT '手机号，首版登录账号',
  password_hash VARCHAR(120) NOT NULL COMMENT 'BCrypt 密码哈希',
  username VARCHAR(64) NOT NULL COMMENT '登录名或系统用户名',
  nickname VARCHAR(64) NOT NULL COMMENT '显示昵称',
  email VARCHAR(128) NULL COMMENT '邮箱',
  gender VARCHAR(16) NULL COMMENT '性别：MALE/FEMALE',
  avatar_code VARCHAR(64) NULL COMMENT '默认头像编码',
  avatar_url VARCHAR(255) NULL COMMENT '头像地址',
  department_id BIGINT NULL COMMENT '所属部门 ID',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_sys_user_phone_deleted (phone, deleted),
  UNIQUE KEY uk_sys_user_username_deleted (username, deleted),
  KEY idx_sys_user_department (department_id),
  KEY idx_sys_user_status (status)
) COMMENT='用户账号表';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  code VARCHAR(64) NOT NULL COMMENT '角色编码',
  name VARCHAR(64) NOT NULL COMMENT '角色名称',
  description VARCHAR(255) NULL COMMENT '角色说明',
  built_in TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置角色',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_sys_role_code_deleted (code, deleted)
) COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  code VARCHAR(128) NOT NULL COMMENT '权限编码',
  name VARCHAR(128) NOT NULL COMMENT '权限名称',
  type VARCHAR(32) NOT NULL COMMENT '权限类型：MENU/BUTTON/API',
  parent_id BIGINT NULL COMMENT '父级权限 ID',
  path VARCHAR(255) NULL COMMENT '前端路由或接口路径',
  method VARCHAR(16) NULL COMMENT '接口方法',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_sys_permission_code_deleted (code, deleted),
  KEY idx_sys_permission_parent (parent_id),
  KEY idx_sys_permission_type (type)
) COMMENT='权限表';

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  role_id BIGINT NOT NULL COMMENT '角色 ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_sys_user_role_deleted (user_id, role_id, deleted),
  KEY idx_sys_user_role_role (role_id)
) COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  role_id BIGINT NOT NULL COMMENT '角色 ID',
  permission_id BIGINT NOT NULL COMMENT '权限 ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_sys_role_permission_deleted (role_id, permission_id, deleted),
  KEY idx_sys_role_permission_permission (permission_id)
) COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS sys_department (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  parent_id BIGINT NULL COMMENT '父级部门 ID',
  name VARCHAR(128) NOT NULL COMMENT '部门名称',
  leader_id BIGINT NULL COMMENT '负责人 ID',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_sys_department_name_deleted (parent_id, name, deleted),
  KEY idx_sys_department_parent (parent_id)
) COMMENT='部门表';

CREATE TABLE IF NOT EXISTS team (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  name VARCHAR(128) NOT NULL COMMENT '团队名称',
  description VARCHAR(255) NULL COMMENT '团队说明',
  processing_scope VARCHAR(255) NULL COMMENT '处理范围',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_team_name_deleted (name, deleted)
) COMMENT='工单处理团队表';

CREATE TABLE IF NOT EXISTS team_member (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  team_id BIGINT NOT NULL COMMENT '团队 ID',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  member_role VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT '团队角色',
  leader_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否负责人',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_team_member_deleted (team_id, user_id, deleted),
  KEY idx_team_member_user (user_id)
) COMMENT='团队成员表';

CREATE TABLE IF NOT EXISTS ticket_category (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  parent_id BIGINT NULL COMMENT '父级分类 ID',
  name VARCHAR(128) NOT NULL COMMENT '分类名称',
  default_team_id BIGINT NULL COMMENT '默认处理团队',
  default_sla_hours INT NULL COMMENT '默认 SLA 小时',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_ticket_category_name_deleted (parent_id, name, deleted),
  KEY idx_ticket_category_parent (parent_id)
) COMMENT='工单分类表';

CREATE TABLE IF NOT EXISTS ticket (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  ticket_no VARCHAR(64) NOT NULL COMMENT '工单编号',
  title VARCHAR(200) NOT NULL COMMENT '工单标题',
  description TEXT NOT NULL COMMENT '问题描述',
  category_id BIGINT NOT NULL COMMENT '分类 ID',
  priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '工单状态',
  creator_id BIGINT NOT NULL COMMENT '创建人 ID',
  assignee_id BIGINT NULL COMMENT '处理人 ID',
  team_id BIGINT NULL COMMENT '处理团队 ID',
  due_time DATETIME NULL COMMENT '截止时间',
  completed_time DATETIME NULL COMMENT '完成时间',
  closed_time DATETIME NULL COMMENT '关闭时间',
  overdue TINYINT NOT NULL DEFAULT 0 COMMENT '是否超时',
  tags VARCHAR(500) NULL COMMENT '标签，逗号分隔或 JSON 字符串',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_ticket_no_deleted (ticket_no, deleted),
  KEY idx_ticket_status_priority_time (status, priority, create_time),
  KEY idx_ticket_creator_time (creator_id, create_time),
  KEY idx_ticket_assignee_status (assignee_id, status),
  KEY idx_ticket_team_status (team_id, status),
  KEY idx_ticket_category (category_id)
) COMMENT='工单主表';

CREATE TABLE IF NOT EXISTS ticket_watch (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  ticket_id BIGINT NOT NULL COMMENT '工单 ID',
  user_id BIGINT NOT NULL COMMENT '关注人 ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_ticket_watch_deleted (ticket_id, user_id, deleted),
  KEY idx_ticket_watch_user (user_id)
) COMMENT='工单关注表';

CREATE TABLE IF NOT EXISTS ticket_comment (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  ticket_id BIGINT NOT NULL COMMENT '工单 ID',
  content TEXT NOT NULL COMMENT '评论内容',
  comment_type VARCHAR(32) NOT NULL DEFAULT 'PUBLIC' COMMENT '评论类型',
  author_id BIGINT NOT NULL COMMENT '评论人 ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  KEY idx_ticket_comment_ticket_time (ticket_id, create_time),
  KEY idx_ticket_comment_author (author_id)
) COMMENT='工单评论表';

CREATE TABLE IF NOT EXISTS ticket_attachment (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
  biz_id BIGINT NOT NULL COMMENT '业务 ID',
  file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  file_size BIGINT NOT NULL COMMENT '文件大小',
  content_type VARCHAR(128) NULL COMMENT 'MIME 类型',
  extension VARCHAR(32) NOT NULL COMMENT '扩展名',
  previewable TINYINT NOT NULL DEFAULT 0 COMMENT '是否可预览',
  preview_type VARCHAR(32) NOT NULL DEFAULT 'DOWNLOAD_ONLY' COMMENT '预览类型',
  download_only TINYINT NOT NULL DEFAULT 1 COMMENT '是否仅下载',
  storage_path VARCHAR(500) NOT NULL COMMENT '本地存储相对路径',
  uploader_id BIGINT NOT NULL COMMENT '上传人 ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  KEY idx_ticket_attachment_biz_time (biz_type, biz_id, create_time),
  KEY idx_ticket_attachment_uploader (uploader_id)
) COMMENT='附件元数据表';

CREATE TABLE IF NOT EXISTS ticket_operation_log (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  ticket_id BIGINT NOT NULL COMMENT '工单 ID',
  operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
  from_status VARCHAR(32) NULL COMMENT '变更前状态',
  to_status VARCHAR(32) NULL COMMENT '变更后状态',
  operator_id BIGINT NOT NULL COMMENT '操作人 ID',
  content VARCHAR(1000) NULL COMMENT '操作说明',
  request_ip VARCHAR(64) NULL COMMENT '请求 IP',
  user_agent VARCHAR(500) NULL COMMENT 'User-Agent',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  KEY idx_ticket_operation_log_ticket_time (ticket_id, create_time),
  KEY idx_ticket_operation_log_operator (operator_id, create_time)
) COMMENT='工单操作日志表';

CREATE TABLE IF NOT EXISTS notification (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  receiver_id BIGINT NOT NULL COMMENT '接收人 ID',
  type VARCHAR(64) NOT NULL COMMENT '通知类型',
  title VARCHAR(200) NOT NULL COMMENT '通知标题',
  content VARCHAR(1000) NOT NULL COMMENT '通知内容',
  biz_type VARCHAR(32) NULL COMMENT '业务类型',
  biz_id BIGINT NULL COMMENT '业务 ID',
  read_status TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
  read_time DATETIME NULL COMMENT '已读时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  KEY idx_notification_receiver_read_time (receiver_id, read_status, create_time),
  KEY idx_notification_biz (biz_type, biz_id)
) COMMENT='站内通知表';

CREATE TABLE IF NOT EXISTS notification_template (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  type VARCHAR(64) NOT NULL COMMENT '通知类型',
  channel VARCHAR(32) NOT NULL DEFAULT 'IN_APP' COMMENT '通知渠道',
  title_template VARCHAR(255) NOT NULL COMMENT '标题模板',
  content_template VARCHAR(1000) NOT NULL COMMENT '内容模板',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_notification_template_type_channel_deleted (type, channel, deleted)
) COMMENT='通知模板表';

CREATE TABLE IF NOT EXISTS knowledge_category (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  parent_id BIGINT NULL COMMENT '父级分类 ID',
  name VARCHAR(128) NOT NULL COMMENT '分类名称',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_knowledge_category_name_deleted (parent_id, name, deleted)
) COMMENT='知识库分类表';

CREATE TABLE IF NOT EXISTS knowledge_tag (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  name VARCHAR(64) NOT NULL COMMENT '标签名称',
  article_count INT NOT NULL DEFAULT 0 COMMENT '文章数量',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_knowledge_tag_name_deleted (name, deleted)
) COMMENT='知识库标签表';

CREATE TABLE IF NOT EXISTS knowledge_article (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  title VARCHAR(200) NOT NULL COMMENT '文章标题',
  summary VARCHAR(500) NULL COMMENT '摘要',
  content LONGTEXT NOT NULL COMMENT '正文',
  category_id BIGINT NULL COMMENT '分类 ID',
  source_ticket_id BIGINT NULL COMMENT '来源工单 ID',
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '文章状态',
  author_id BIGINT NOT NULL COMMENT '作者 ID',
  view_count BIGINT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  published_time DATETIME NULL COMMENT '发布时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  KEY idx_knowledge_article_category (category_id),
  KEY idx_knowledge_article_status_time (status, create_time),
  KEY idx_knowledge_article_author (author_id)
) COMMENT='知识库文章表';

CREATE TABLE IF NOT EXISTS knowledge_article_tag (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  article_id BIGINT NOT NULL COMMENT '文章 ID',
  tag_id BIGINT NOT NULL COMMENT '标签 ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_knowledge_article_tag_deleted (article_id, tag_id, deleted)
) COMMENT='知识库文章标签关联表';

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  operator_id BIGINT NULL COMMENT '操作人 ID',
  operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
  biz_type VARCHAR(32) NULL COMMENT '业务类型',
  biz_id BIGINT NULL COMMENT '业务 ID',
  content VARCHAR(1000) NULL COMMENT '操作内容',
  request_ip VARCHAR(64) NULL COMMENT '请求 IP',
  user_agent VARCHAR(500) NULL COMMENT 'User-Agent',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  KEY idx_audit_log_operator_time (operator_id, create_time),
  KEY idx_audit_log_biz_time (biz_type, biz_id, create_time)
) COMMENT='系统审计日志表';

CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  config_key VARCHAR(128) NOT NULL COMMENT '配置键',
  config_value TEXT NULL COMMENT '配置值',
  config_group VARCHAR(64) NOT NULL COMMENT '配置分组',
  description VARCHAR(255) NULL COMMENT '配置说明',
  editable TINYINT NOT NULL DEFAULT 1 COMMENT '是否可编辑',
  `sensitive` TINYINT NOT NULL DEFAULT 0 COMMENT '是否敏感',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_system_config_key_deleted (config_key, deleted),
  KEY idx_system_config_group (config_group)
) COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS sla_rule (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  category_id BIGINT NOT NULL COMMENT '工单分类 ID',
  priority VARCHAR(32) NOT NULL COMMENT '优先级',
  response_hours INT NOT NULL COMMENT '响应小时数',
  resolve_hours INT NOT NULL COMMENT '解决小时数',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  UNIQUE KEY uk_sla_rule_category_priority_deleted (category_id, priority, deleted)
) COMMENT='SLA 规则表';

CREATE TABLE IF NOT EXISTS ai_call_log (
  id BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
  scene VARCHAR(64) NOT NULL COMMENT 'AI 场景',
  biz_type VARCHAR(32) NULL COMMENT '业务类型',
  biz_id BIGINT NULL COMMENT '业务 ID',
  operator_id BIGINT NULL COMMENT '调用人 ID',
  prompt_tokens INT NULL COMMENT '提示词 token',
  completion_tokens INT NULL COMMENT '输出 token',
  cost DECIMAL(12, 4) NULL COMMENT '调用成本',
  duration_ms BIGINT NULL COMMENT '耗时毫秒',
  desensitized TINYINT NOT NULL DEFAULT 1 COMMENT '是否已脱敏',
  success TINYINT NOT NULL DEFAULT 0 COMMENT '是否成功',
  error_message VARCHAR(1000) NULL COMMENT '错误信息',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by BIGINT NULL COMMENT '创建人 ID',
  update_by BIGINT NULL COMMENT '更新人 ID',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 删除',
  KEY idx_ai_call_log_scene_time (scene, create_time),
  KEY idx_ai_call_log_biz (biz_type, biz_id)
) COMMENT='AI 调用日志表';
