DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
        THEN
            ALTER TABLE skjonnsfastsetting_sykepengegrunnlag
                OWNER TO "spesialist-db-migrations";
        END IF;
    END
$$;