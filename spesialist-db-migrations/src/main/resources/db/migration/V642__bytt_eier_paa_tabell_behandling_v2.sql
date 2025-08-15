DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
        THEN
            ALTER TABLE behandling_v2
                OWNER TO "spesialist-db-migrations";
        END IF;
    END
$$;