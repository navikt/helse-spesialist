DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-sidegig')
    THEN
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "spesialist-sidegig";
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "spesialist-sidegig";
    END IF;
END$$;

DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-sidegig')
    THEN
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO "spesialist-sidegig";
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO "spesialist-sidegig";
    END IF;
END $$;
