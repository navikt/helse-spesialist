DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqlsuperuser')
    THEN
        GRANT ALL PRIVILEGES ON TABLE behandling_soknad TO cloudsqlsuperuser;
    END IF;
END$$;