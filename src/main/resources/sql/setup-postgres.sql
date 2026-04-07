-- =============================================================
-- dbdadi - PostgreSQL setup script
-- Run as a PostgreSQL superuser (e.g. postgres)
-- Usage: psql -U postgres -f setup-postgres.sql
-- =============================================================

-- 1. Create the application user
CREATE USER dbdadi WITH PASSWORD 'dbdadi';

-- 2. Create the database owned by the application user
CREATE DATABASE dbdadi OWNER dbdadi;

-- 3. Connect to the new database
\connect dbdadi

-- 4. Grant all privileges on the public schema
--    Required on PostgreSQL 15+ where CREATE is no longer
--    granted to PUBLIC by default
GRANT ALL ON SCHEMA public TO dbdadi;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO dbdadi;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO dbdadi;

-- 5. Ensure future tables and sequences are also accessible
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON TABLES TO dbdadi;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON SEQUENCES TO dbdadi;

-- =============================================================
-- Hibernate (ddl-auto=update) will create all application
-- tables automatically on first startup.
-- Start the backend with:
--   ./start-dbdadi.sh postgres
-- =============================================================
