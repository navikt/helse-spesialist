DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqlsuperuser')
        THEN
            GRANT ALL PRIVILEGES ON TYPE
                generasjon_tilstand,
                mottakertype,
                notattype,
                oppgave_type,
                oppgavestatus,
                oppgavetype,
                overstyringtype,
                person_kjonn,
                totrinnsvurdering_tilstand,
                utbetaling_status,
                utbetaling_type,
                warning_kilde
                TO cloudsqlsuperuser;
        END IF;
    END
$$;

DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'cloudsqlsuperuser')
        THEN
            ALTER DEFAULT PRIVILEGES FOR USER cloudsqlsuperuser IN SCHEMA public GRANT ALL PRIVILEGES ON TYPES TO cloudsqlsuperuser;
        END IF;
    END
$$;
