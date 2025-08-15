DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
        THEN
            ALTER TABLE selve_varsel
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE skjonnsfastsetting_sykepengegrunnlag
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE snapshot
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE spesialsak
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE stans_automatisering
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE stans_automatisk_behandling_saksbehandler
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE tildeling
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE tilkommen_inntekt_events
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE totrinnsvurdering
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE utbetaling
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE utbetaling_id
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE utbetalingslinje
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE vedtak
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE vedtak_begrunnelse
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE vedtaksperiode_hendelse
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE vedtaksperiode_utbetaling_id
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE vergemal
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE vilkarsgrunnlag_per_avviksvurdering
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE vilkarsgrunnlag_per_avviksvurdering_spinnvillgate
                OWNER TO "spesialist-db-migrations";
        END IF;
    END
$$;
