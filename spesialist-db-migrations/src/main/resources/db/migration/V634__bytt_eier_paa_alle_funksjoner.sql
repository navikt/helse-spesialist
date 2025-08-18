DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
        THEN
            ALTER FUNCTION oppdater_oppdatert_kolonne() OWNER TO "spesialist-db-migrations";
            ALTER FUNCTION kast_exception() OWNER TO "spesialist-db-migrations";
            ALTER FUNCTION pgaudit_ddl_command_end() OWNER TO "spesialist-db-migrations";
            ALTER FUNCTION pgaudit_sql_drop() OWNER TO "spesialist-db-migrations";
        END IF;
    END
$$;
