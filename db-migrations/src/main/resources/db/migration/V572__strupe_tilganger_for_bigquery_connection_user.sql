DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'bigquery_connection_user')
    THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM bigquery_connection_user;
        REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM bigquery_connection_user;

        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public REVOKE ALL ON TABLES FROM bigquery_connection_user;
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public REVOKE ALL PRIVILEGES ON SEQUENCES FROM bigquery_connection_user;

        GRANT SELECT ON ALL TABLES IN SCHEMA public TO bigquery_connection_user;
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public GRANT SELECT ON TABLES TO bigquery_connection_user;
    END IF;
END$$;
