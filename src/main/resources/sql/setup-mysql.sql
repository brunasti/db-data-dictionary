-- =============================================================
-- dbdadi - MySQL setup script
-- Run as root or a MySQL admin user
-- Usage: mysql -u root -p < setup-mysql.sql
-- =============================================================

-- 1. Create the database
CREATE DATABASE IF NOT EXISTS dbdadi
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Create the application user
CREATE USER IF NOT EXISTS 'dbdadi'@'localhost' IDENTIFIED BY 'dbdadi';

-- 3. Grant all privileges on the application database
GRANT ALL PRIVILEGES ON dbdadi.* TO 'dbdadi'@'localhost';

FLUSH PRIVILEGES;

-- =============================================================
-- Hibernate (ddl-auto=update) will create all application
-- tables automatically on first startup.
-- Start the backend with:
--   ./start-dbdadi.sh mysql
-- =============================================================
