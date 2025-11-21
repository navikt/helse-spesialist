DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO cloudsqliamuser;
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
        END IF;
    END
$$;
DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqliamuser')
    THEN
        ALTER DEFAULT PRIVILEGES FOR USER "spesialist-db-migrations" IN SCHEMA public GRANT SELECT ON SEQUENCES TO cloudsqliamuser;
        ALTER DEFAULT PRIVILEGES FOR USER "spesialist-db-migrations" IN SCHEMA public GRANT SELECT ON TABLES TO cloudsqliamuser;
    END IF;
END $$;
