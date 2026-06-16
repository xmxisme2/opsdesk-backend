-- OpsDesk 本地库迁移脚本：将 ticket.ticket_no 调整为可空。
-- 背景：工单采用 B 方案，草稿阶段不生成编号，提交时再生成唯一 ticketNo。
-- 执行方式：请使用 mysql 客户端 source 方式执行，避免 PowerShell 管道导致中文 SQL 乱码。

SET NAMES utf8mb4;
USE opsdesk;

ALTER TABLE ticket
  MODIFY ticket_no VARCHAR(64) NULL COMMENT '工单编号，草稿可为空，提交时生成';
