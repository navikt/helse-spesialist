UPDATE oppgave
SET
    mottaker = CASE
                   WHEN mottaker IS NULL AND egenskaper @> ARRAY['UTBETALING_TIL_SYKMELDT']::varchar[] THEN 'UtbetalingTilSykmeldt'
                   WHEN mottaker IS NULL AND egenskaper @> ARRAY['DELVIS_REFUSJON']::varchar[] THEN 'DelvisRefusjon'
                   WHEN mottaker IS NULL AND egenskaper @> ARRAY['UTBETALING_TIL_ARBEIDSGIVER']::varchar[] THEN 'UtbetalingTilArbeidsgiver'
                   WHEN mottaker IS NULL AND egenskaper @> ARRAY['INGEN_UTBETALING']::varchar[] THEN 'IngenUtbetaling'
                   ELSE mottaker
        END,
    oppgavetype = CASE
                      WHEN oppgavetype IS NULL AND egenskaper @> ARRAY['REVURDERING']::varchar[] THEN 'Revurdering'
                      WHEN oppgavetype IS NULL AND egenskaper @> ARRAY['SØKNAD']::varchar[] THEN 'Søknad'
                      ELSE oppgavetype
        END,
    inntektskilde = CASE
                        WHEN inntektskilde IS NULL AND egenskaper @> ARRAY['EN_ARBEIDSGIVER']::varchar[] THEN 'EnArbeidsgiver'
                        WHEN inntektskilde IS NULL AND egenskaper @> ARRAY['FLERE_ARBEIDSGIVERE']::varchar[] THEN 'FlereArbeidsgivere'
                        ELSE inntektskilde
        END,
    inntektsforhold = CASE
                          WHEN inntektsforhold IS NULL AND egenskaper @> ARRAY['ARBEIDSTAKER']::varchar[] THEN 'Arbeidstaker'
                          WHEN inntektsforhold IS NULL AND egenskaper @> ARRAY['SELVSTENDIG_NÆRINGSDRIVENDE']::varchar[] THEN 'SelvstendigNæringsdrivende'
                          ELSE inntektsforhold
        END,
    periodetype = CASE
                      WHEN periodetype IS NULL AND egenskaper @> ARRAY['FORSTEGANGSBEHANDLING']::varchar[] THEN 'Førstegangsbehandling'
                      WHEN periodetype IS NULL AND egenskaper @> ARRAY['FORLENGELSE']::varchar[] THEN 'Forlengelse'
                      WHEN periodetype IS NULL AND egenskaper @> ARRAY['INFOTRYGDFORLENGELSE']::varchar[] THEN 'Infotrygdforlengelse'
                      WHEN periodetype IS NULL AND egenskaper @> ARRAY['OVERGANG_FRA_IT']::varchar[] THEN 'OvergangFraIt'
                      ELSE periodetype
        END
WHERE mottaker IS NULL
   OR oppgavetype IS NULL
   OR inntektskilde IS NULL
   OR inntektsforhold IS NULL
   OR periodetype IS NULL;
