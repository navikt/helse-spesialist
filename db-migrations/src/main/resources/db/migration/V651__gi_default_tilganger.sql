DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqlsuperuser')
    THEN
        ALTER DEFAULT PRIVILEGES FOR USER "spesialist-db-migrations" IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO cloudsqlsuperuser;
        ALTER DEFAULT PRIVILEGES FOR USER "spesialist-db-migrations" IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO cloudsqlsuperuser;
    END IF;
END $$;
