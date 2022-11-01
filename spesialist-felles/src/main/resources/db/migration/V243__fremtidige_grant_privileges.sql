DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-opprydding-dev')
    THEN
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO "spesialist-opprydding-dev";
        ALTER DEFAULT PRIVILEGES FOR USER spesialist IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO "spesialist-opprydding-dev";
    END IF;
END $$;
