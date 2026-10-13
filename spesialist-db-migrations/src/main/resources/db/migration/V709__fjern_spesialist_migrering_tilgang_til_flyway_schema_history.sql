DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-migrering')
    THEN
        REVOKE ALL PRIVILEGES ON TABLE public.flyway_schema_history FROM "spesialist-migrering";
    END IF;
END$$;
