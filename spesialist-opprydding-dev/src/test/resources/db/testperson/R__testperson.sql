INSERT INTO saksbehandler(oid, navn, epost, ident) VALUES ('${saksbehandler_oid}', 'SAKSBEHANDLER SAKSBEHANDLERSEN', 'saksbehandler@nav.no', 'I123456');

INSERT INTO hendelse(id, data, type, fodselsnummer) VALUES ('${hendelse_id}', '{}'::json, 'TESTHENDELSE', ${fødselsnummer});
INSERT INTO vedtaksperiode_hendelse(hendelse_ref, vedtaksperiode_id) VALUES ('${hendelse_id}', '${vedtaksperiode_id}');
INSERT INTO command_context(context_id, hendelse_id, tilstand, data) VALUES ('${command_context_id}', '${hendelse_id}', 'TESTTILSTAND', '{}'::json);
INSERT INTO person_info(id, fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse) VALUES (${sequence_number}, 'NAVN', 'MELLOMNAVN', 'NAVNESEN', '2018-01-01', 'Ukjent', 'NEI');
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
INSERT INTO vedtak(id, vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, snapshot_ref)
VALUES (${sequence_number}, '${vedtaksperiode_id}', now(), now(), ${sequence_number}, ${sequence_number},
        ${sequence_number});
INSERT INTO warning(id, melding, vedtak_ref, kilde, opprettet)
VALUES (${sequence_number}, 'WARNING', ${sequence_number}, 'Spesialist', now());
INSERT INTO saksbehandleroppgavetype(id, type, vedtak_ref, inntektskilde)
VALUES (${sequence_number}, 'SØKNAD', ${sequence_number}, 'EN_ARBEIDSGIVER');
INSERT INTO vedtaksperiode_utbetaling_id(vedtaksperiode_id, utbetaling_id)
VALUES ('${vedtaksperiode_id}', '${utbetaling_id}');
INSERT INTO egen_ansatt(person_ref, er_egen_ansatt, opprettet)
VALUES (${sequence_number}, false, now());
INSERT INTO vergemal(person_ref, har_vergemal, har_fremtidsfullmakter, har_fullmakter, opprettet)
VALUES (${sequence_number}, false, false, false, now());
INSERT INTO digital_kontaktinformasjon(person_ref, er_digital, opprettet)
VALUES (${sequence_number}, true, now());
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
                    command_context_id, type, utbetaling_id)
VALUES (${sequence_number}, now(), now(), 'AvventerSystem', ${sequence_number}, null, null, '${command_context_id}',
        'SØKNAD', '${utbetaling_id}');

INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref, på_vent) VALUES ('${saksbehandler_oid}', ${sequence_number}, false);
INSERT INTO reserver_person(saksbehandler_ref, person_ref, gyldig_til) VALUES ('${saksbehandler_oid}', ${sequence_number}, now());

INSERT INTO opptegnelse(person_id, sekvensnummer, payload, type) VALUES (${sequence_number}, 1, '{}'::json, 'TESTTYPE');
INSERT INTO abonnement_for_opptegnelse(saksbehandler_id, person_id, siste_sekvensnummer) VALUES ('${saksbehandler_oid}', ${sequence_number}, 1);

INSERT INTO notat(id, tekst, opprettet, saksbehandler_oid, vedtaksperiode_id, feilregistrert, feilregistrert_tidspunkt) VALUES (${sequence_number}, 'TEST_TEXT', now(), '${saksbehandler_oid}', '${vedtaksperiode_id}', false, now());
INSERT INTO overstyring(id, begrunnelse, tidspunkt, person_ref, arbeidsgiver_ref, hendelse_id, saksbehandler_ref) VALUES (${sequence_number}, 'BEGRUNNELSE', now(), ${sequence_number}, ${sequence_number}, '${hendelse_id}', '${saksbehandler_oid}');
INSERT INTO overstyrtdag(id, overstyring_ref, dato, dagtype, grad) VALUES (${sequence_number}, ${sequence_number}, '2018-01-01', 'TESTDAGTYPE', 100);
INSERT INTO overstyring_inntekt(id, tidspunkt, person_ref, arbeidsgiver_ref, saksbehandler_ref, hendelse_ref, begrunnelse, manedlig_inntekt, skjaeringstidspunkt, forklaring) VALUES (${sequence_number}, now(), ${sequence_number}, ${sequence_number}, '${saksbehandler_oid}', '${hendelse_id}', 'BEGRUNNELSE', 1000, '2018-01-01', 'FORKLARING');
INSERT INTO overstyring_arbeidsforhold(id, tidspunkt, person_ref, arbeidsgiver_ref, saksbehandler_ref, hendelse_ref, begrunnelse, forklaring, deaktivert, skjaeringstidspunkt) VALUES (${sequence_number}, now(), ${sequence_number}, ${sequence_number}, '${saksbehandler_oid}', '${hendelse_id}', 'BEGRUNNELSE', 'FORKLARING', false, '2018-01-01');

INSERT INTO annullert_av_saksbehandler(id, annullert_tidspunkt, saksbehandler_ref) VALUES (${sequence_number}, now(), '${saksbehandler_oid}');
INSERT INTO oppdrag(id, fagsystem_id, mottaker, fagområde, endringskode, sistearbeidsgiverdag) VALUES (${sequence_number}, 'EN_PERSON_FAGSYSTEMID', 'MOTTAKER', 'SP', 'NY', '2018-01-01');
INSERT INTO oppdrag(id, fagsystem_id, mottaker, fagområde, endringskode, sistearbeidsgiverdag) VALUES (${sequence_number} + 1000, 'EN_ARBEIDSGIVER_FAGSYSTEMID', 'MOTTAKER', 'SPREF', 'NY', '2018-01-01');
INSERT INTO utbetalingslinje(id, oppdrag_id, delytelseid, refdelytelseid, reffagsystemid, endringskode, klassekode, statuskode, datostatusfom, fom, tom, dagsats, lønn, grad, totalbeløp) VALUES (${sequence_number}, ${sequence_number}, 1, null, null, 'NY', 'SPREFAG-IOP', null, null, '2018-01-01', '2018-01-31', 1000, 1000, 100, 1000);
INSERT INTO utbetalingslinje(id, oppdrag_id, delytelseid, refdelytelseid, reffagsystemid, endringskode, klassekode, statuskode, datostatusfom, fom, tom, dagsats, lønn, grad, totalbeløp) VALUES (${sequence_number} + 1000, ${sequence_number} + 1000, 1, null, null, 'NY', 'SPREFAG-IOP', null, null, '2018-01-01', '2018-01-31', 1000, 1000, 100, 1000);
INSERT INTO utbetaling_id(id, utbetaling_id, person_ref, arbeidsgiver_ref, arbeidsgiver_fagsystem_id_ref, person_fagsystem_id_ref, type, opprettet) VALUES (${sequence_number}, '${utbetaling_id}', ${sequence_number}, ${sequence_number}, ${sequence_number} + 1000, ${sequence_number}, 'UTBETALING', now());

INSERT INTO utbetaling(id, status, opprettet, data, utbetaling_id_ref, annullert_av_saksbehandler_ref) VALUES (${sequence_number}, 'UTBETALT', now(), '{}'::json, ${sequence_number}, ${sequence_number});

INSERT INTO risikovurdering(id, vedtaksperiode_id, samlet_score, ufullstendig, opprettet) VALUES (${sequence_number}, '${vedtaksperiode_id}', 1000, false, now());
INSERT INTO risikovurdering_arbeidsuforhetvurdering(id, risikovurdering_ref, tekst) VALUES (${sequence_number}, ${sequence_number}, 'TESTTEKST');
INSERT INTO risikovurdering_faresignal(id, risikovurdering_ref, tekst) VALUES (${sequence_number}, ${sequence_number}, 'TESTTEKST');

INSERT INTO feilende_meldinger(id, event_name, opprettet, blob) VALUES (gen_random_uuid(), 'FEILENDE_TESTHENDELSE', now(), '{}'::json);
