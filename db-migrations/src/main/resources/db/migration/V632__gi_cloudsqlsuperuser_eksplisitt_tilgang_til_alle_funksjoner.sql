DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqlsuperuser')
    THEN
        GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO cloudsqlsuperuser;
    END IF;
END$$;

DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqlsuperuser')
    THEN
        ALTER DEFAULT PRIVILEGES FOR USER cloudsqlsuperuser IN SCHEMA public GRANT ALL PRIVILEGES ON FUNCTIONS TO cloudsqlsuperuser;
    END IF;
END $$;
