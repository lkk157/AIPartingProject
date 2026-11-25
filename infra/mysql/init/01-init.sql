-- P0 基础设施初始化
-- 数据库由 MYSQL_DATABASE 环境变量创建；此处仅做字符集与时区兜底
CREATE DATABASE IF NOT EXISTS parking
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
