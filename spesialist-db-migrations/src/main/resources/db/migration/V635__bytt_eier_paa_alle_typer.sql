DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
        THEN
            ALTER TYPE generasjon_tilstand OWNER TO "spesialist-db-migrations";
            ALTER TYPE mottakertype OWNER TO "spesialist-db-migrations";
            ALTER TYPE notattype OWNER TO "spesialist-db-migrations";
            ALTER TYPE oppgave_type OWNER TO "spesialist-db-migrations";
            ALTER TYPE oppgavestatus OWNER TO "spesialist-db-migrations";
            ALTER TYPE oppgavetype OWNER TO "spesialist-db-migrations";
            ALTER TYPE overstyringtype OWNER TO "spesialist-db-migrations";
            ALTER TYPE person_kjonn OWNER TO "spesialist-db-migrations";
            ALTER TYPE totrinnsvurdering_tilstand OWNER TO "spesialist-db-migrations";
            ALTER TYPE utbetaling_status OWNER TO "spesialist-db-migrations";
            ALTER TYPE utbetaling_type OWNER TO "spesialist-db-migrations";
            ALTER TYPE warning_kild OWNER TO "spesialist-db-migrations";
        END IF;
    END
$$;
