-- OpsDesk 初始化数据 UTF-8 校验脚本
-- 用于确认基础中文种子数据没有被 PowerShell 管道或客户端编码转换成问号。

USE opsdesk;

SET NAMES utf8mb4;

SELECT 'sys_role' AS table_name, COUNT(*) AS garbled_count
FROM sys_role
WHERE name REGEXP '\\?{2,}' OR description REGEXP '\\?{2,}'
UNION ALL
SELECT 'sys_permission' AS table_name, COUNT(*) AS garbled_count
FROM sys_permission
WHERE name REGEXP '\\?{2,}'
UNION ALL
SELECT 'sys_department' AS table_name, COUNT(*) AS garbled_count
FROM sys_department
WHERE name REGEXP '\\?{2,}'
UNION ALL
SELECT 'team' AS table_name, COUNT(*) AS garbled_count
FROM team
WHERE name REGEXP '\\?{2,}' OR description REGEXP '\\?{2,}' OR processing_scope REGEXP '\\?{2,}'
UNION ALL
SELECT 'ticket_category' AS table_name, COUNT(*) AS garbled_count
FROM ticket_category
WHERE name REGEXP '\\?{2,}';
