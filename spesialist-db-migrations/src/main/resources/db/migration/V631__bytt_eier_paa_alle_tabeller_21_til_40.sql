DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
        THEN
            ALTER TABLE generasjon_begrunnelse_kobling
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE global_snapshot_versjon
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE gosysoppgaver
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE hendelse
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE infotrygdutbetalinger
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE inntekt
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE kommentarer
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE melding_duplikatkontroll
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE notat
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE oppdrag
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE oppgave
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE oppgave_behandling_kobling
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE opprinnelig_soknadsdato
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE opptegnelse
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE overstyring
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE overstyring_arbeidsforhold
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE overstyring_dag
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE overstyring_inntekt
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE overstyring_minimum_sykdomsgrad
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE overstyring_minimum_sykdomsgrad_arbeidsgiver
                OWNER TO "spesialist-db-migrations";
        END IF;
    END
$$;
