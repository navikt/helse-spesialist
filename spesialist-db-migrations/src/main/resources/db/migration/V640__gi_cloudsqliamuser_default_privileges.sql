DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT ALL PRIVILEGES ON TABLE ukoblede_annulleringer TO cloudsqliamuser;

            ALTER DEFAULT PRIVILEGES FOR USER cloudsqliamuser IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO cloudsqliamuser;
            ALTER DEFAULT PRIVILEGES FOR USER cloudsqliamuser IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO cloudsqliamuser;

            ALTER DEFAULT PRIVILEGES FOR USER cloudsqliamuser IN SCHEMA public GRANT ALL PRIVILEGES ON TYPES TO cloudsqliamuser;
        END IF;
    END
$$;