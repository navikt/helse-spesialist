DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
    THEN
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "spesialist-db-migrations";
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "spesialist-db-migrations";
    END IF;
END$$;

DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
    THEN
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO "spesialist-db-migrations";
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO "spesialist-db-migrations";
    END IF;
END $$;
