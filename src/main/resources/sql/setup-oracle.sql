-- =============================================================
-- dbdadi - Oracle setup script
-- Run as SYSDBA
-- Usage: sqlplus sys/password@//localhost:1521/XE as sysdba @setup-oracle.sql
-- =============================================================

-- 1. Create the tablespace (optional, adjust paths as needed)
-- CREATE TABLESPACE dbdadi_ts
--     DATAFILE 'dbdadi.dbf' SIZE 100M AUTOEXTEND ON;

-- 2. Create the application user / schema
CREATE USER dbdadi IDENTIFIED BY dbdadi
    DEFAULT TABLESPACE users
    TEMPORARY TABLESPACE temp
    QUOTA UNLIMITED ON users;

-- 3. Grant required privileges
GRANT CONNECT, RESOURCE TO dbdadi;
GRANT CREATE SESSION TO dbdadi;
GRANT CREATE TABLE TO dbdadi;
GRANT CREATE SEQUENCE TO dbdadi;
GRANT CREATE VIEW TO dbdadi;
GRANT CREATE INDEX TO dbdadi;

-- =============================================================
-- NOTE: Add the Oracle JDBC driver (ojdbc11) to pom.xml before
-- starting. It is not available on Maven Central.
-- Download from: https://www.oracle.com/database/technologies/jdbc-downloads.html
--
-- Hibernate (ddl-auto=update) will create all application
-- tables automatically on first startup.
-- Start the backend with:
--   ./start-dbdadi.sh oracle
-- =============================================================
