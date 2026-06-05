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

INSERT INTO sys_permission (id, code, name, type, parent_id, path, method, sort, enabled, create_by, update_by)
VALUES
  (1000, 'MENU_WORKBENCH', '工作台', 'MENU', NULL, '/workbench', NULL, 10, 1, NULL, NULL),
  (1100, 'MENU_TICKETS', '工单中心', 'MENU', NULL, '/tickets', NULL, 20, 1, NULL, NULL),
  (1110, 'MENU_TICKETS_ALL', '工单列表', 'MENU', 1100, '/tickets', NULL, 21, 1, NULL, NULL),
  (1120, 'MENU_MY_TICKETS', '我的工单', 'MENU', 1100, '/my-tickets', NULL, 22, 1, NULL, NULL),
  (1200, 'MENU_NOTIFICATIONS', '通知中心', 'MENU', NULL, '/notifications', NULL, 30, 1, NULL, NULL),
  (1300, 'MENU_KNOWLEDGE', '知识库', 'MENU', NULL, '/knowledge', NULL, 40, 1, NULL, NULL),
  (1400, 'MENU_DASHBOARD', '数据看板', 'MENU', NULL, '/dashboard', NULL, 50, 1, NULL, NULL),
  (1500, 'MENU_SYSTEM', '系统管理', 'MENU', NULL, '/system', NULL, 90, 1, NULL, NULL),
  (1510, 'MENU_SYSTEM_USERS', '用户管理', 'MENU', 1500, '/system/users', NULL, 91, 1, NULL, NULL),
  (1520, 'MENU_SYSTEM_ROLES', '角色管理', 'MENU', 1500, '/system/roles', NULL, 92, 1, NULL, NULL),
  (1530, 'MENU_SYSTEM_ORG', '组织管理', 'MENU', 1500, '/system/org', NULL, 93, 1, NULL, NULL),
  (1540, 'MENU_SYSTEM_TICKET_CATEGORY', '工单分类', 'MENU', 1500, '/system/ticket-categories', NULL, 94, 1, NULL, NULL),
  (1550, 'MENU_SYSTEM_AUDIT', '操作日志', 'MENU', 1500, '/system/audit', NULL, 95, 1, NULL, NULL),
  (1560, 'MENU_SYSTEM_CONFIG', '系统配置', 'MENU', 1500, '/system/configs', NULL, 96, 1, NULL, NULL),
  (2000, 'BUTTON_ROLE_CREATE', '创建角色', 'BUTTON', 1520, NULL, NULL, 201, 1, NULL, NULL),
  (2001, 'BUTTON_ROLE_UPDATE', '编辑角色', 'BUTTON', 1520, NULL, NULL, 202, 1, NULL, NULL),
  (2002, 'BUTTON_ROLE_DELETE', '删除角色', 'BUTTON', 1520, NULL, NULL, 203, 1, NULL, NULL),
  (2003, 'BUTTON_ROLE_PERMISSION_UPDATE', '分配角色权限', 'BUTTON', 1520, NULL, NULL, 204, 1, NULL, NULL),
  (2004, 'BUTTON_USER_ROLE_UPDATE', '分配用户角色', 'BUTTON', 1510, NULL, NULL, 205, 1, NULL, NULL),
  (3000, 'API_ROLES_SEARCH', '角色列表接口', 'API', 1520, '/api/roles/search', 'POST', 301, 1, NULL, NULL),
  (3001, 'API_ROLES_CREATE', '创建角色接口', 'API', 1520, '/api/roles/create', 'POST', 302, 1, NULL, NULL),
  (3002, 'API_ROLES_DETAIL', '角色详情接口', 'API', 1520, '/api/roles/{id}/detail', 'POST', 303, 1, NULL, NULL),
  (3003, 'API_ROLES_UPDATE', '编辑角色接口', 'API', 1520, '/api/roles/{id}/update', 'POST', 304, 1, NULL, NULL),
  (3004, 'API_ROLES_DELETE', '删除角色接口', 'API', 1520, '/api/roles/{id}/delete', 'POST', 305, 1, NULL, NULL),
  (3005, 'API_ROLES_PERMISSIONS_UPDATE', '分配角色权限接口', 'API', 1520, '/api/roles/{id}/permissions/update', 'POST', 306, 1, NULL, NULL),
  (3006, 'API_PERMISSIONS_TREE', '权限树接口', 'API', 1520, '/api/permissions/tree', 'POST', 307, 1, NULL, NULL),
  (3007, 'API_USERS_ROLES_UPDATE', '用户绑定角色接口', 'API', 1510, '/api/users/{id}/roles/update', 'POST', 308, 1, NULL, NULL)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  type = VALUES(type),
  parent_id = VALUES(parent_id),
  path = VALUES(path),
  method = VALUES(method),
  sort = VALUES(sort),
  enabled = VALUES(enabled),
  deleted = 0,
  update_time = CURRENT_TIMESTAMP;

INSERT INTO sys_role_permission (id, role_id, permission_id, create_by, update_by)
VALUES
  (10000, 1, 1000, NULL, NULL),
  (10001, 1, 1100, NULL, NULL),
  (10002, 1, 1110, NULL, NULL),
  (10003, 1, 1120, NULL, NULL),
  (10004, 1, 1200, NULL, NULL),
  (10005, 1, 1300, NULL, NULL),
  (10006, 1, 1400, NULL, NULL),
  (10007, 1, 1500, NULL, NULL),
  (10008, 1, 1510, NULL, NULL),
  (10009, 1, 1520, NULL, NULL),
  (10010, 1, 1530, NULL, NULL),
  (10011, 1, 1540, NULL, NULL),
  (10012, 1, 1550, NULL, NULL),
  (10013, 1, 1560, NULL, NULL),
  (10014, 1, 2000, NULL, NULL),
  (10015, 1, 2001, NULL, NULL),
  (10016, 1, 2002, NULL, NULL),
  (10017, 1, 2003, NULL, NULL),
  (10018, 1, 2004, NULL, NULL),
  (10019, 1, 3000, NULL, NULL),
  (10020, 1, 3001, NULL, NULL),
  (10021, 1, 3002, NULL, NULL),
  (10022, 1, 3003, NULL, NULL),
  (10023, 1, 3004, NULL, NULL),
  (10024, 1, 3005, NULL, NULL),
  (10025, 1, 3006, NULL, NULL),
  (10026, 1, 3007, NULL, NULL),
  (11000, 2, 1000, NULL, NULL),
  (11001, 2, 1100, NULL, NULL),
  (11002, 2, 1110, NULL, NULL),
  (11003, 2, 1120, NULL, NULL),
  (11004, 2, 1200, NULL, NULL),
  (11005, 2, 1300, NULL, NULL),
  (11006, 2, 1400, NULL, NULL),
  (12000, 3, 1000, NULL, NULL),
  (12001, 3, 1100, NULL, NULL),
  (12002, 3, 1110, NULL, NULL),
  (12003, 3, 1120, NULL, NULL),
  (12004, 3, 1200, NULL, NULL),
  (12005, 3, 1300, NULL, NULL),
  (13000, 4, 1000, NULL, NULL),
  (13001, 4, 1100, NULL, NULL),
  (13002, 4, 1120, NULL, NULL),
  (13003, 4, 1200, NULL, NULL)
ON DUPLICATE KEY UPDATE
  deleted = 0,
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
