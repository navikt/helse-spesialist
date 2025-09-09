DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqlsuperuser')
    THEN
        GRANT ALL PRIVILEGES ON TABLE ukoblede_annulleringer TO cloudsqlsuperuser;
    END IF;
END$$;