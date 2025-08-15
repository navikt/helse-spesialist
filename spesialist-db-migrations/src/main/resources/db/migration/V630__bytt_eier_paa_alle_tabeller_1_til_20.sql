DO
$$
    BEGIN
        IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spesialist-db-migrations')
        THEN
            ALTER TABLE abonnement_for_opptegnelse
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE annullert_av_saksbehandler
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE api_varseldefinisjon
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE arbeidsforhold
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE arbeidsgiver
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE automatisering
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE automatisering_korrigert_soknad
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE automatisering_problem
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE avviksvurdering
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE avviksvurdering_spinnvillgate
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE begrunnelse
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE behandling
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE behandling_v2
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE command_context
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE dialog
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE dokumenter
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE egen_ansatt
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE enhet
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE flyway_schema_history
                OWNER TO "spesialist-db-migrations";
            ALTER TABLE force_automatisering
                OWNER TO "spesialist-db-migrations";
        END IF;
    END
$$;
