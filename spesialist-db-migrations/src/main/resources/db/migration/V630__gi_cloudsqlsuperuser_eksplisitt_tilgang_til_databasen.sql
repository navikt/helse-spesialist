DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqlsuperuser')
    THEN
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO cloudsqlsuperuser;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO cloudsqlsuperuser;
    END IF;
END$$;

DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqlsuperuser')
    THEN
        ALTER DEFAULT PRIVILEGES FOR USER cloudsqlsuperuser IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO cloudsqlsuperuser;
        ALTER DEFAULT PRIVILEGES FOR USER cloudsqlsuperuser IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO cloudsqlsuperuser;
    END IF;
END $$;
