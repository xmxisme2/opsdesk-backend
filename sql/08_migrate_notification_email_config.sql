-- 邮件通知配置：旧环境执行一次即可，默认关闭以避免未配置 SMTP 时误发邮件。
INSERT INTO system_config (id, config_key, config_value, config_group, description, editable, `sensitive`, create_by, update_by)
VALUES
  (12, 'notification.email.enabled', 'false', 'NOTIFICATION', '邮件通知开关；开启后将所有通知投递至默认邮箱', 1, 0, NULL, NULL),
  (13, 'notification.email.default_recipient', 'sean.siu@astralotus.com', 'NOTIFICATION', '邮件通知开启时使用的默认收件邮箱', 1, 0, NULL, NULL)
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  description = VALUES(description),
  editable = VALUES(editable),
  `sensitive` = VALUES(`sensitive`),
  deleted = 0,
  update_time = CURRENT_TIMESTAMP;
