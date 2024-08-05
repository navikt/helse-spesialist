DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'bigquery_connection_user')
    THEN
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "bigquery_connection_user";
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "bigquery_connection_user";
    END IF;
END$$;

DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'bigquery_connection_user')
    THEN
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO "bigquery_connection_user";
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO "bigquery_connection_user";
    END IF;
END $$;
