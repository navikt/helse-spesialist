DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
            AND EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist')
        THEN
            -- Eierskap kan bare endres av nåværende eier (spesialist) eller et medlem av rollen
            GRANT "spesialist" TO CURRENT_USER;
            ALTER TABLE flyway_schema_history
                OWNER TO "spesialist-db-migrations";
            REVOKE "spesialist" FROM CURRENT_USER;
        END IF;
    END
$$;
