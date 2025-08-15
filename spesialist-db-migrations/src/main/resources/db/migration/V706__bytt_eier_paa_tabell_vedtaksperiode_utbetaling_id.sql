DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
        THEN
            ALTER TABLE vedtaksperiode_utbetaling_id
                OWNER TO "spesialist-db-migrations";
        END IF;
    END
$$;