DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist')
    THEN
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO spesialist;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO spesialist;
    END IF;
END$$;

DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist')
    THEN
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO spesialist;
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO spesialist;
    END IF;
END $$;
