-- OpsDesk 初始化数据脚本
-- 管理员登录口径：手机号 13800000000，密码 root123456。

USE opsdesk;

SET NAMES utf8mb4;

INSERT INTO sys_role (id, code, name, description, built_in, enabled, create_by, update_by)
VALUES
  (1, 'ADMIN', '管理员', '拥有系统全部管理权限', 1, 1, NULL, NULL),
  (2, 'MANAGER', '团队负责人', '负责团队工单分派、看板和知识库维护', 1, 1, NULL, NULL),
  (3, 'AGENT', '处理人', '负责处理分派到个人或团队的工单', 1, 1, NULL, NULL),
  (4, 'USER', '普通用户', '提交和跟踪自己的工单', 1, 1, NULL, NULL)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  built_in = VALUES(built_in),
  enabled = VALUES(enabled),
  update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_user (
  id, phone, password_hash, username, nickname, email, gender, avatar_code, avatar_url,
  department_id, status, create_by, update_by
)
VALUES (
  1,
  '13800000000',
  '$2a$10$veqlubgQZMTPAoQ5vdc7weo5pI3w.6Kkaj5/WPyH/WJ4sCRyIwqvO',
  'admin',
  '系统管理员',
  'admin@opsdesk.local',
  'MALE',
  'avatar_admin',
  NULL,
  1,
  'ACTIVE',
  NULL,
  NULL
)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  nickname = VALUES(nickname),
  email = VALUES(email),
  status = VALUES(status),
  update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_user_role (id, user_id, role_id, create_by, update_by)
VALUES (1, 1, 1, NULL, NULL)
ON DUPLICATE KEY UPDATE
  update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_department (id, parent_id, name, leader_id, sort, enabled, create_by, update_by)
VALUES
  (1, NULL, 'OpsDesk 公司', 1, 1, 1, NULL, NULL),
  (2, 1, 'IT 部', 1, 10, 1, NULL, NULL),
  (3, 1, '运维部', 1, 20, 1, NULL, NULL),
  (4, 1, '研发部', 1, 30, 1, NULL, NULL),
  (5, 1, '财务部', 1, 40, 1, NULL, NULL),
  (6, 1, '人力行政', 1, 50, 1, NULL, NULL)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  leader_id = VALUES(leader_id),
  sort = VALUES(sort),
  enabled = VALUES(enabled),
  update_time = CURRENT_TIMESTAMP;

INSERT INTO team (id, name, description, processing_scope, enabled, create_by, update_by)
VALUES
  (1, '基础设施支持组', '处理网络、服务器、账号和权限类问题', '账号问题,网络访问,设备故障', 1, NULL, NULL),
  (2, '应用支持组', '处理业务系统和应用故障类问题', '业务系统,系统故障', 1, NULL, NULL),
  (3, '数据支持组', '处理数据查询、报表和数据质量问题', '数据问题', 1, NULL, NULL)
ON DUPLICATE KEY UPDATE
  description = VALUES(description),
  processing_scope = VALUES(processing_scope),
  enabled = VALUES(enabled),
  update_time = CURRENT_TIMESTAMP;

INSERT INTO team_member (id, team_id, user_id, member_role, leader_flag, create_by, update_by)
VALUES
  (1, 1, 1, 'LEADER', 1, NULL, NULL),
  (2, 2, 1, 'LEADER', 1, NULL, NULL),
  (3, 3, 1, 'LEADER', 1, NULL, NULL)
ON DUPLICATE KEY UPDATE
  member_role = VALUES(member_role),
  leader_flag = VALUES(leader_flag),
  update_time = CURRENT_TIMESTAMP;

INSERT INTO ticket_category (id, parent_id, name, default_team_id, default_sla_hours, sort, enabled, create_by, update_by)
VALUES
  (1, NULL, '账号问题', 1, 24, 10, 1, NULL, NULL),
  (2, NULL, '系统故障', 2, 8, 20, 1, NULL, NULL),
  (3, NULL, '网络访问', 1, 12, 30, 1, NULL, NULL),
  (4, NULL, '业务系统', 2, 16, 40, 1, NULL, NULL),
  (5, NULL, '数据问题', 3, 24, 50, 1, NULL, NULL),
  (6, NULL, '权限申请', 1, 24, 60, 1, NULL, NULL),
  (7, NULL, '设备故障', 1, 24, 70, 1, NULL, NULL)
ON DUPLICATE KEY UPDATE
  default_team_id = VALUES(default_team_id),
  default_sla_hours = VALUES(default_sla_hours),
  sort = VALUES(sort),
  enabled = VALUES(enabled),
  update_time = CURRENT_TIMESTAMP;

INSERT INTO sla_rule (id, category_id, priority, response_hours, resolve_hours, enabled, create_by, update_by)
VALUES
  (1, 1, 'LOW', 8, 72, 1, NULL, NULL),
  (2, 1, 'MEDIUM', 4, 24, 1, NULL, NULL),
  (3, 1, 'HIGH', 2, 12, 1, NULL, NULL),
  (4, 1, 'URGENT', 1, 4, 1, NULL, NULL)
ON DUPLICATE KEY UPDATE
  response_hours = VALUES(response_hours),
  resolve_hours = VALUES(resolve_hours),
  enabled = VALUES(enabled),
  update_time = CURRENT_TIMESTAMP;

INSERT INTO notification_template (id, type, channel, title_template, content_template, enabled, create_by, update_by)
VALUES
  (1, 'TICKET_ASSIGNED', 'IN_APP', '工单已分派', '工单 {ticketNo} 已分派给你，请及时处理。', 1, NULL, NULL),
  (2, 'TICKET_COMMENTED', 'IN_APP', '工单有新评论', '工单 {ticketNo} 收到新评论，请查看。', 1, NULL, NULL),
  (3, 'TICKET_STATUS_CHANGED', 'IN_APP', '工单状态已变更', '工单 {ticketNo} 状态已变更为 {status}。', 1, NULL, NULL),
  (4, 'TICKET_OVERDUE', 'IN_APP', '工单已超时', '工单 {ticketNo} 已超时，请尽快跟进。', 1, NULL, NULL),
  (5, 'TICKET_CLOSED', 'IN_APP', '工单已关闭', '工单 {ticketNo} 已关闭归档。', 1, NULL, NULL)
ON DUPLICATE KEY UPDATE
  title_template = VALUES(title_template),
  content_template = VALUES(content_template),
  enabled = VALUES(enabled),
  update_time = CURRENT_TIMESTAMP;

INSERT INTO system_config (id, config_key, config_value, config_group, description, editable, `sensitive`, create_by, update_by)
VALUES
  (1, 'ai.enabled', 'false', 'AI', 'AI 功能默认关闭，一期不接入真实生成能力', 1, 0, NULL, NULL),
  (2, 'notification.in_app.enabled', 'true', 'NOTIFICATION', '站内通知开关', 1, 0, NULL, NULL),
  (3, 'upload.max_file_size_mb', '20', 'UPLOAD', '单文件最大大小 MB', 1, 0, NULL, NULL),
  (4, 'upload.max_files_per_ticket', '10', 'UPLOAD', '单工单最大附件数量', 1, 0, NULL, NULL),
  (5, 'upload.allowed_extensions', 'jpg,jpeg,png,pdf,docx,xlsx,txt,log,zip', 'UPLOAD', '允许上传的扩展名', 1, 0, NULL, NULL)
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  description = VALUES(description),
  editable = VALUES(editable),
  `sensitive` = VALUES(`sensitive`),
  update_time = CURRENT_TIMESTAMP;
