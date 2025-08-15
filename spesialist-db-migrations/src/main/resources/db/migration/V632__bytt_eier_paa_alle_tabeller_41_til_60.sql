DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
        THEN
            ALTER TABLE overstyring_minimum_sykdomsgrad_periode
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE overstyring_tidslinje
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE overstyring_tilkommen_inntekt
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE overstyringer_for_vedtaksperioder
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE pa_vent
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE periodehistorikk
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE person
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE person_info
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE person_klargjores
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE poison_pill
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE reserver_person
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE risikovurdering
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE risikovurdering_2021
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE risikovurdering_arbeidsuforhetvurdering
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE risikovurdering_faresignal
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE saksbehandler
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE saksbehandler_opptegnelse_sekvensnummer
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE saksbehandleroppgavetype
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE sammenligningsgrunnlag
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE sammenligningsgrunnlag_spinnvillgate
                OWNER TO "spesialist-db-migrations";
        END IF;
    END
$$;
