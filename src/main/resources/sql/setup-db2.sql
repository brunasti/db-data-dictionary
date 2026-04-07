-- =============================================================
-- dbdadi - DB2 setup script
-- Run as DB2 instance owner or a user with SYSADM authority
-- Usage: db2 -tvf setup-db2.sql
-- =============================================================

-- 1. Create the database
CREATE DATABASE dbdadi USING CODESET UTF-8 TERRITORY US;

-- Connect to the new database
CONNECT TO dbdadi;

-- 2. Create the application schema
CREATE SCHEMA dbdadi AUTHORIZATION dbdadi;

-- 3. Grant privileges
GRANT DBADM ON DATABASE TO USER dbdadi;
GRANT USE OF TABLESPACE USERSPACE1 TO USER dbdadi;
GRANT CREATETAB ON DATABASE TO USER dbdadi;
GRANT IMPLICIT_SCHEMA ON DATABASE TO USER dbdadi;

CONNECT RESET;

-- =============================================================
-- NOTE: Add the DB2 JDBC driver (db2jcc4) to pom.xml before
-- starting. It is not available on Maven Central.
-- Download from: https://www.ibm.com/support/pages/node/382667
--
-- Hibernate (ddl-auto=update) will create all application
-- tables automatically on first startup.
-- Start the backend with:
--   ./start-dbdadi.sh db2
-- =============================================================
