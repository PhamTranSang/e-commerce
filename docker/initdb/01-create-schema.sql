-- Bootstrap schema for local development.
--
-- The postgres image runs every file in /docker-entrypoint-initdb.d/ exactly
-- ONCE, on the first initialization of the data volume (i.e. when the data dir
-- is empty). It is executed against POSTGRES_DB as POSTGRES_USER.
--
-- The application connects with `?currentSchema=ecommerce`, so this schema must
-- exist before Liquibase runs (Liquibase creates its lock/log tables in it).
CREATE SCHEMA IF NOT EXISTS ecommerce;
