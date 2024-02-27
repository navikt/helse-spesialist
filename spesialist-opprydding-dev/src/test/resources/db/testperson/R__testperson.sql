INSERT INTO saksbehandler(oid, navn, epost, ident)
VALUES ('${saksbehandler_oid}', 'SAKSBEHANDLER SAKSBEHANDLERSEN', 'saksbehandler@nav.no', 'I123456');

INSERT INTO hendelse(id, data, type, fodselsnummer)
VALUES ('${hendelse_id}', '{}'::json, 'TESTHENDELSE', ${fødselsnummer});
INSERT INTO vedtaksperiode_hendelse(hendelse_ref, vedtaksperiode_id)
VALUES ('${hendelse_id}', '${vedtaksperiode_id}');
INSERT INTO command_context(context_id, hendelse_id, tilstand, data)
VALUES ('${command_context_id}', '${hendelse_id}', 'TESTTILSTAND', '{}'::json);
INSERT INTO person_info(id, fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
VALUES (${sequence_number}, 'NAVN', 'MELLOMNAVN', 'NAVNESEN', '2018-01-01', 'Ukjent', 'NEI');
INSERT INTO infotrygdutbetalinger(id, data)
VALUES (${sequence_number}, '{}'::json);
INSERT INTO person(id, fodselsnummer, aktor_id, info_ref, enhet_ref, enhet_ref_oppdatert, personinfo_oppdatert,
                   infotrygdutbetalinger_ref, infotrygdutbetalinger_oppdatert)
VALUES (${sequence_number}, ${fødselsnummer}, ${aktør_id}, ${sequence_number}, 101, now(), now(), ${sequence_number},
        now());
INSERT INTO arbeidsgiver_navn(id, navn, navn_oppdatert)
VALUES (${sequence_number}, 'ARBEIDSGIVER', '2018-01-01');
INSERT INTO arbeidsgiver_bransjer(id, bransjer, oppdatert)
VALUES (${sequence_number}, 'BRANSJE', now());
INSERT INTO arbeidsgiver(id, orgnummer, navn_ref, bransjer_ref)
VALUES (${sequence_number}, ${organisasjonsnummer}, ${sequence_number}, ${sequence_number});
INSERT INTO arbeidsforhold(id, person_ref, arbeidsgiver_ref, startdato, sluttdato, stillingstittel, stillingsprosent,
                           oppdatert)
VALUES (${sequence_number}, ${sequence_number}, ${sequence_number}, '2018-01-01', '2018-01-31', 'STILLING', 100, now());
UPDATE global_snapshot_versjon
SET versjon = 1;
INSERT INTO snapshot(id, data, person_ref, versjon)
VALUES (${sequence_number}, '{}'::json, ${sequence_number}, 1);
INSERT INTO vedtak(id, vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, snapshot_ref, forkastet)
VALUES (${sequence_number}, '${vedtaksperiode_id}', now(), now(), ${sequence_number}, ${sequence_number},
        ${sequence_number}, false);
INSERT INTO selve_vedtaksperiode_generasjon(id, unik_id, vedtaksperiode_id, opprettet_av_hendelse, tilstand)
VALUES (${sequence_number}, '${generasjon_id}', '${vedtaksperiode_id}', '${hendelse_id}', 'Ulåst');
INSERT INTO opprinnelig_soknadsdato (vedtaksperiode_id, soknad_mottatt)
VALUES ('${vedtaksperiode_id}', now());
INSERT INTO selve_varsel(unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref)
VALUES (gen_random_uuid(), 'EN_KODE', '${vedtaksperiode_id}', now(), ${sequence_number});
INSERT INTO saksbehandleroppgavetype(id, type, vedtak_ref, inntektskilde)
VALUES (${sequence_number}, 'SØKNAD', ${sequence_number}, 'EN_ARBEIDSGIVER');
INSERT INTO vedtaksperiode_utbetaling_id(vedtaksperiode_id, utbetaling_id)
VALUES ('${vedtaksperiode_id}', '${utbetaling_id}');
INSERT INTO egen_ansatt(person_ref, er_egen_ansatt, opprettet)
VALUES (${sequence_number}, false, now());
INSERT INTO vergemal(person_ref, har_vergemal, har_fremtidsfullmakter, har_fullmakter, opprettet)
VALUES (${sequence_number}, false, false, false, now());
INSERT INTO gosysoppgaver(person_ref, antall, oppslag_feilet, opprettet)
VALUES (${sequence_number}, 0, false, now());
INSERT INTO risikovurdering_2021(id, vedtaksperiode_id, kan_godkjennes_automatisk, krever_supersaksbehandler, data,
                                 opprettet)
VALUES (${sequence_number}, '${vedtaksperiode_id}', false, false, '{}'::json, now());
INSERT INTO automatisering(vedtaksperiode_ref, hendelse_ref, automatisert, stikkprøve, opprettet, utbetaling_id)
VALUES (${sequence_number}, '${hendelse_id}', false, false, now(), '${utbetaling_id}');
INSERT INTO automatisering_problem(id, vedtaksperiode_ref, hendelse_ref, problem)
VALUES (${sequence_number}, ${sequence_number}, '${hendelse_id}', 'PROBLEM');
INSERT INTO oppgave(id, opprettet, oppdatert, status, vedtak_ref, ferdigstilt_av, ferdigstilt_av_oid,
                    command_context_id, utbetaling_id, kan_avvises)
VALUES (${sequence_number}, now(), now(), 'AvventerSystem', ${sequence_number}, null, null, '${command_context_id}', '${utbetaling_id}', true);

INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref)
VALUES ('${saksbehandler_oid}', ${sequence_number});
INSERT INTO reserver_person(saksbehandler_ref, person_ref, gyldig_til)
VALUES ('${saksbehandler_oid}', ${sequence_number}, now());

INSERT INTO pa_vent(id, vedtaksperiode_id, saksbehandler_ref, frist, begrunnelse, opprettet)
VALUES (${sequence_number}, '${vedtaksperiode_id}', '${saksbehandler_oid}', now(), '', now());

INSERT INTO opptegnelse(person_id, sekvensnummer, payload, type)
VALUES (${sequence_number}, 1, '{}'::json, 'TESTTYPE');
INSERT INTO abonnement_for_opptegnelse(saksbehandler_id, person_id)
VALUES ('${saksbehandler_oid}', ${sequence_number});
INSERT INTO saksbehandler_opptegnelse_sekvensnummer (saksbehandler_id, siste_sekvensnummer)
VALUES ('${saksbehandler_oid}', 1);
INSERT INTO dokumenter(dokument_id, person_ref, dokument, opprettet)
VALUES (gen_random_uuid(), ${sequence_number}, '{}'::json, now());

INSERT INTO notat(id, tekst, opprettet, saksbehandler_oid, vedtaksperiode_id, feilregistrert, feilregistrert_tidspunkt)
VALUES (${sequence_number}, 'TEST_TEXT', now(), '${saksbehandler_oid}', '${vedtaksperiode_id}', false, now());
INSERT INTO kommentarer(tekst, notat_ref, feilregistrert_tidspunkt, saksbehandlerident)
VALUES ('EN_KOMMENTAR', ${sequence_number}, null, '${saksbehandler_oid}');

INSERT INTO overstyring(id, tidspunkt, person_ref, hendelse_ref, saksbehandler_ref)
VALUES (${sequence_number}, now(), ${sequence_number}, '${hendelse_id}',
        '${saksbehandler_oid}');
INSERT INTO overstyring_tidslinje(id, overstyring_ref, arbeidsgiver_ref, begrunnelse)
VALUES (${sequence_number}, ${sequence_number}, ${sequence_number},'BEGRUNNELSE');
INSERT INTO overstyring_dag(id, dato, dagtype, grad, overstyring_tidslinje_ref)
VALUES (${sequence_number}, '2018-01-01', 'TESTDAGTYPE', 100, ${sequence_number});
INSERT INTO overstyring_inntekt(id, overstyring_ref, manedlig_inntekt, skjaeringstidspunkt, forklaring, begrunnelse, arbeidsgiver_ref)
VALUES (${sequence_number}, ${sequence_number}, 1000, '2018-01-01', 'FORKLARING', 'BEGRUNNELSE', ${sequence_number});
INSERT INTO overstyring_arbeidsforhold(id, overstyring_ref, forklaring, deaktivert, skjaeringstidspunkt, begrunnelse, arbeidsgiver_ref)
VALUES (${sequence_number}, ${sequence_number}, 'FORKLARING', false, '2018-01-01', 'BEGRUNNELSE', ${sequence_number});

INSERT INTO begrunnelse(id, tekst, type, saksbehandler_ref) VALUES(${sequence_number}, 'Begrunnelsefritekst', 'SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_FRITEKST', '${saksbehandler_oid}');
INSERT INTO begrunnelse(id, tekst, type, saksbehandler_ref) VALUES(${sequence_number} + 1000, 'En begrunnelsemal', 'SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_MAL', '${saksbehandler_oid}');
INSERT INTO begrunnelse(id, tekst, type, saksbehandler_ref) VALUES(${sequence_number} + 2000, 'En begrunnelsekonklusjon', 'SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_KONKLUSJON', '${saksbehandler_oid}');
INSERT INTO generasjon_begrunnelse_kobling(generasjon_id, begrunnelse_id) VALUES ('${generasjon_id}', ${sequence_number});
INSERT INTO skjonnsfastsetting_sykepengegrunnlag(id, skjaeringstidspunkt, arsak, overstyring_ref, begrunnelse_fritekst_ref, begrunnelse_mal_ref, begrunnelse_konklusjon_ref)
VALUES (${sequence_number}, '2018-01-01', 'ÅRSAK', ${sequence_number}, ${sequence_number}, ${sequence_number} + 1000, ${sequence_number} + 2000);
INSERT INTO skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver(id, arlig, fra_arlig, arbeidsgiver_ref, skjonnsfastsetting_sykepengegrunnlag_ref)
VALUES (${sequence_number}, 1000, 1200, ${sequence_number}, ${sequence_number});
INSERT INTO overstyringer_for_vedtaksperioder(vedtaksperiode_id, overstyring_ref)
VALUES ('${vedtaksperiode_id}', ${sequence_number});

INSERT INTO annullert_av_saksbehandler(id, annullert_tidspunkt, saksbehandler_ref)
VALUES (${sequence_number}, now(), '${saksbehandler_oid}');
INSERT INTO oppdrag(id, fagsystem_id, mottaker)
VALUES (${sequence_number}, 'EN_PERSON_FAGSYSTEMID', 'MOTTAKER');
INSERT INTO oppdrag(id, fagsystem_id, mottaker)
VALUES (${sequence_number} + 1000, 'EN_ARBEIDSGIVER_FAGSYSTEMID', 'MOTTAKER');
INSERT INTO utbetalingslinje(id, oppdrag_id, fom, tom, totalbeløp)
VALUES (${sequence_number}, ${sequence_number}, '2018-01-01', '2018-01-31', 1000);
INSERT INTO utbetalingslinje(id, oppdrag_id, fom, tom, totalbeløp)
VALUES (${sequence_number} + 1000, ${sequence_number} + 1000, '2018-01-01', '2018-01-31', 1000);
INSERT INTO utbetaling_id(id, utbetaling_id, person_ref, arbeidsgiver_ref, arbeidsgiver_fagsystem_id_ref,
                          person_fagsystem_id_ref, type, opprettet, personbeløp, arbeidsgiverbeløp)
VALUES (${sequence_number}, '${utbetaling_id}', ${sequence_number}, ${sequence_number}, ${sequence_number} + 1000,
        ${sequence_number}, 'UTBETALING', now(), 0, 0);

INSERT INTO utbetaling(id, status, opprettet, data, utbetaling_id_ref, annullert_av_saksbehandler_ref)
VALUES (${sequence_number}, 'UTBETALT', now(), '{}'::json, ${sequence_number}, ${sequence_number});

INSERT INTO totrinnsvurdering(id, vedtaksperiode_id, er_retur, saksbehandler, beslutter, utbetaling_id_ref, opprettet, oppdatert)
VALUES (${sequence_number}, '${vedtaksperiode_id}', false, '${saksbehandler_oid}', '${saksbehandler_oid}', ${sequence_number}, now(), null);

INSERT INTO periodehistorikk(id, type, timestamp, utbetaling_id, saksbehandler_oid, notat_id)
VALUES (${sequence_number}, 'TOTRINNSVURDERING_RETUR', now(), '${utbetaling_id}', '${saksbehandler_oid}',
        ${sequence_number});

INSERT INTO risikovurdering(id, vedtaksperiode_id, samlet_score, ufullstendig, opprettet)
VALUES (${sequence_number}, '${vedtaksperiode_id}', 1000, false, now());
INSERT INTO risikovurdering_arbeidsuforhetvurdering(id, risikovurdering_ref, tekst)
VALUES (${sequence_number}, ${sequence_number}, 'TESTTEKST');
INSERT INTO risikovurdering_faresignal(id, risikovurdering_ref, tekst)
VALUES (${sequence_number}, ${sequence_number}, 'TESTTEKST');

INSERT INTO feilende_meldinger(id, event_name, opprettet, blob)
VALUES (gen_random_uuid(), 'FEILENDE_TESTHENDELSE', now(), '{}'::json);

INSERT INTO oppgave_behandling_kobling(oppgave_id, behandling_id)
VALUES (${sequence_number}, gen_random_uuid());

INSERT INTO stottetabell_for_skjonnsmessig_fastsettelse(fodselsnummer)
VALUES (${fødselsnummer});

INSERT INTO sammenligningsgrunnlag(id, fødselsnummer, skjæringstidspunkt, opprettet, sammenligningsgrunnlag)
VALUES (${sequence_number}, ${fødselsnummer}, '2018-01-01', now(), '{}'::json);

INSERT INTO avviksvurdering(id, unik_id, fødselsnummer, skjæringstidspunkt, opprettet, avviksprosent, beregningsgrunnlag, sammenligningsgrunnlag_ref)
VALUES (${sequence_number}, '${avviksvurdering_unik_id}', ${fødselsnummer}, '2018-01-01', now(), 25.0, '{}'::json, ${sequence_number});

INSERT INTO vilkarsgrunnlag_per_avviksvurdering(avviksvurdering_ref, vilkårsgrunnlag_id)
VALUES ('${avviksvurdering_unik_id}', gen_random_uuid());
