package no.nav.helse.opprydding

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.db.testfixtures.ModuleIsolatedDBTestFixture
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.random.Random

internal abstract class AbstractDatabaseTest {
    protected val dataSource = OppryddingDevDBTestFixture.fixture.module.dataSource
    protected val personRepository = PersonRepository(dataSource)

    protected fun opprettPerson(
        fødselsnummer: String,
        sequenceNumber: Int = 1,
    ) {
        val sequence_number = sequenceNumber.toString()
        val periodehistorikk_dialog_id = Random.nextInt(1000, 99999).toString()
        val saksbehandler_oid = UUID.randomUUID().toString()
        val hendelse_id = UUID.randomUUID().toString()
        val generasjon_id = UUID.randomUUID().toString()
        val vedtaksperiode_id = UUID.randomUUID().toString()
        val command_context_id = UUID.randomUUID().toString()
        val aktør_id = fødselsnummer.reversed()
        val organisasjonsnummer = Random.nextInt(100000000, 999999999).toString()
        val utbetaling_id = UUID.randomUUID().toString()
        val avviksvurdering_unik_id = UUID.randomUUID().toString()
        val spleisBehandlingId = UUID.randomUUID().toString()

        @Language("PostgreSQL")
        val sql = """
        INSERT INTO saksbehandler(oid, navn, epost, ident)
        VALUES ('${saksbehandler_oid}', 'SAKSBEHANDLER SAKSBEHANDLERSEN', 'saksbehandler@nav.no', 'I123456');
        
        INSERT INTO hendelse(id, data, type)
        VALUES ('${hendelse_id}', '{"fødselsnummer": "${fødselsnummer}"}'::json, 'TESTHENDELSE');
        INSERT INTO vedtaksperiode_hendelse(hendelse_ref, vedtaksperiode_id)
        VALUES ('${hendelse_id}', '${vedtaksperiode_id}');
        INSERT INTO command_context(context_id, hendelse_id, opprettet, tilstand, data)
        VALUES ('${command_context_id}', '${hendelse_id}', now(), 'SUSPENDERT', '{}'::json);
        INSERT INTO person_info(id, fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
        VALUES (${sequence_number}, 'NAVN', 'MELLOMNAVN', 'NAVNESEN', '2018-01-01', 'Ukjent', 'NEI');
        INSERT INTO person_klargjores(fødselsnummer, opprettet) VALUES (${fødselsnummer}, now());
        INSERT INTO infotrygdutbetalinger(id, data)
        VALUES (${sequence_number}, '{}'::json);
        INSERT INTO person(id, fødselsnummer, aktør_id, info_ref, enhet_ref, enhet_ref_oppdatert, personinfo_oppdatert,
                           infotrygdutbetalinger_ref, infotrygdutbetalinger_oppdatert)
        VALUES (${sequence_number}, '${fødselsnummer}', '${aktør_id}', ${sequence_number}, 101, now(), now(), ${sequence_number},
                now());
        INSERT INTO personpseudoid(pseudoid, identitetsnummer)
        VALUES (gen_random_uuid(), '${fødselsnummer}');
        INSERT INTO arbeidsgiver(identifikator, navn, navn_sist_oppdatert_dato)
        VALUES ('${organisasjonsnummer}', 'ARBEIDSGIVER', '2018-01-01');
        INSERT INTO arbeidsforhold(id, person_ref, arbeidsgiver_identifikator, startdato, sluttdato, stillingstittel, stillingsprosent, oppdatert)
        VALUES (${sequence_number}, ${sequence_number}, '${organisasjonsnummer}', '2018-01-01', '2018-01-31', 'STILLING', 100, now());
        UPDATE global_snapshot_versjon
        SET versjon = 1 WHERE versjon <> 1; -- WHERE for å slippe varsel om "update without 'where' updates all rows at once
        INSERT INTO snapshot(id, data, person_ref, versjon)
        VALUES (${sequence_number}, '{}'::json, ${sequence_number}, 1);
        INSERT INTO vedtak(id, vedtaksperiode_id, fom, tom, arbeidsgiver_identifikator, person_ref, forkastet)
        VALUES (${sequence_number}, '${vedtaksperiode_id}', now(), now(), '${organisasjonsnummer}', ${sequence_number}, false);
        INSERT INTO behandling(id, unik_id, vedtaksperiode_id, opprettet_av_hendelse, tilstand, spleis_behandling_id)
        VALUES (${sequence_number}, '${generasjon_id}', '${vedtaksperiode_id}', '${hendelse_id}', 'VidereBehandlingAvklares', '${spleisBehandlingId}');
        INSERT INTO behandling_soknad(behandling_id, søknad_id) 
        VALUES ('${spleisBehandlingId}', '${UUID.randomUUID()}');
        INSERT INTO behandling_v2(vedtaksperiode_id, behandling_id, fom, tom, skjæringstidspunkt, opprettet)
        VALUES ('${vedtaksperiode_id}', gen_random_uuid(), now(), now(), now(), now());
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
        INSERT INTO vergemal(person_ref, har_vergemal, har_fremtidsfullmakter, har_fullmakter, vergemål_oppdatert, fullmakt_oppdatert)
        VALUES (${sequence_number}, false, false, false, now(), now());
        INSERT INTO gosysoppgaver(person_ref, antall, oppslag_feilet, opprettet)
        VALUES (${sequence_number}, 0, false, now());
        INSERT INTO risikovurdering_2021(id, vedtaksperiode_id, kan_godkjennes_automatisk, data, opprettet)
        VALUES (${sequence_number}, '${vedtaksperiode_id}', false, '{}'::json, now());
        INSERT INTO stans_automatisering
        VALUES (${sequence_number}, ${fødselsnummer}, 'STOPP_AUTOMATIKK', '{}', now(), 'ISYFO', '{}');
        INSERT INTO stans_automatisk_behandling_saksbehandler
        VALUES (${fødselsnummer}, now());
        INSERT INTO automatisering(vedtaksperiode_ref, hendelse_ref, automatisert, stikkprøve, opprettet, utbetaling_id)
        VALUES (${sequence_number}, '${hendelse_id}', false, false, now(), '${utbetaling_id}');
        INSERT INTO automatisering_problem(id, vedtaksperiode_ref, hendelse_ref, problem)
        VALUES (${sequence_number}, ${sequence_number}, '${hendelse_id}', 'PROBLEM');
        INSERT INTO oppgave(id, opprettet, første_opprettet, oppdatert, status, vedtak_ref, ferdigstilt_av, ferdigstilt_av_oid,
                            hendelse_id_godkjenningsbehov, utbetaling_id, kan_avvises)
        VALUES (${sequence_number}, now(), now(), now(), 'AvventerSystem', ${sequence_number}, null, null, '${hendelse_id}', '${utbetaling_id}', true);
        
        INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref)
        VALUES ('${saksbehandler_oid}', ${sequence_number});
        INSERT INTO reserver_person(saksbehandler_ref, person_ref, gyldig_til)
        VALUES ('${saksbehandler_oid}', ${sequence_number}, now());
        
        INSERT INTO opptegnelse(person_id, sekvensnummer, payload, type)
        VALUES (${sequence_number}, 1, '{}'::json, 'TESTTYPE');
        INSERT INTO abonnement_for_opptegnelse(saksbehandler_id, person_id)
        VALUES ('${saksbehandler_oid}', ${sequence_number});
        INSERT INTO saksbehandler_opptegnelse_sekvensnummer (saksbehandler_id, siste_sekvensnummer)
        VALUES ('${saksbehandler_oid}', 1);
        INSERT INTO dokumenter(dokument_id, person_ref, dokument, opprettet)
        VALUES (gen_random_uuid(), ${sequence_number}, '{}'::json, now());
        
        INSERT INTO dialog(id, opprettet)
        VALUES (${sequence_number}, now());
        INSERT INTO notat(id, tekst, opprettet, saksbehandler_oid, vedtaksperiode_id, feilregistrert, feilregistrert_tidspunkt, dialog_ref)
        VALUES (${sequence_number}, 'TEST_TEXT', now(), '${saksbehandler_oid}', '${vedtaksperiode_id}', false, now(), ${sequence_number});
        INSERT INTO kommentarer(tekst, feilregistrert_tidspunkt, saksbehandlerident, dialog_ref)
        VALUES ('EN_KOMMENTAR', null, '${saksbehandler_oid}', ${sequence_number});
        
        INSERT INTO overstyring(id, tidspunkt, person_ref, hendelse_ref, saksbehandler_ref, vedtaksperiode_id)
        VALUES (${sequence_number}, now(), ${sequence_number}, '${hendelse_id}',
                '${saksbehandler_oid}', '${vedtaksperiode_id}');
        INSERT INTO overstyring_tidslinje(id, overstyring_ref, arbeidsgiver_identifikator, begrunnelse)
        VALUES (${sequence_number}, ${sequence_number}, '${organisasjonsnummer}', 'BEGRUNNELSE');
        INSERT INTO overstyring_tilkommen_inntekt(id, overstyring_ref, json)
        VALUES (${sequence_number}, ${sequence_number}, '{}');
        INSERT INTO overstyring_dag(id, dato, dagtype, grad, overstyring_tidslinje_ref)
        VALUES (${sequence_number}, '2018-01-01', 'TESTDAGTYPE', 100, ${sequence_number});
        INSERT INTO overstyring_inntekt(id, overstyring_ref, manedlig_inntekt, skjaeringstidspunkt, forklaring, begrunnelse, arbeidsgiver_identifikator, fom, tom)
        VALUES (${sequence_number}, ${sequence_number}, 1000, '2018-01-01', 'FORKLARING', 'BEGRUNNELSE', '${organisasjonsnummer}', '2018-01-01', null);
        INSERT INTO overstyring_arbeidsforhold(id, overstyring_ref, forklaring, deaktivert, skjaeringstidspunkt, begrunnelse, arbeidsgiver_identifikator)
        VALUES (${sequence_number}, ${sequence_number}, 'FORKLARING', false, '2018-01-01', 'BEGRUNNELSE', '${organisasjonsnummer}');
        INSERT INTO overstyring_minimum_sykdomsgrad(id, overstyring_ref, fom, tom, vurdering, begrunnelse)
        VALUES (${sequence_number}, ${sequence_number}, '2018-01-01', '2018-01-31', true, 'En begrunnelse');
        INSERT INTO overstyring_minimum_sykdomsgrad_periode(id, fom, tom, vurdering, overstyring_minimum_sykdomsgrad_ref)
        VALUES (${sequence_number}, '2018-01-01', '2018-01-31', true, ${sequence_number});
        INSERT INTO overstyring_minimum_sykdomsgrad_arbeidsgiver(id, berort_vedtaksperiode_id, arbeidsgiver_identifikator, overstyring_minimum_sykdomsgrad_ref)
        VALUES (${sequence_number}, '${vedtaksperiode_id}', '${organisasjonsnummer}', ${sequence_number});
        
        INSERT INTO begrunnelse(id, tekst, type, saksbehandler_ref) VALUES(${sequence_number}, 'Begrunnelsefritekst', 'SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_FRITEKST', '${saksbehandler_oid}');
        INSERT INTO begrunnelse(id, tekst, type, saksbehandler_ref) VALUES(${sequence_number} + 1000, 'En begrunnelsemal', 'SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_MAL', '${saksbehandler_oid}');
        INSERT INTO begrunnelse(id, tekst, type, saksbehandler_ref) VALUES(${sequence_number} + 2000, 'En begrunnelsekonklusjon', 'SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_KONKLUSJON', '${saksbehandler_oid}');
        INSERT INTO generasjon_begrunnelse_kobling(generasjon_id, begrunnelse_id) VALUES ('${generasjon_id}', ${sequence_number});
        INSERT INTO skjonnsfastsetting_sykepengegrunnlag(id, skjaeringstidspunkt, arsak, overstyring_ref, begrunnelse_fritekst_ref, begrunnelse_mal_ref, begrunnelse_konklusjon_ref)
        VALUES (${sequence_number}, '2018-01-01', 'ÅRSAK', ${sequence_number}, ${sequence_number}, ${sequence_number} + 1000, ${sequence_number} + 2000);
        INSERT INTO skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver(id, arlig, fra_arlig, arbeidsgiver_identifikator, skjonnsfastsetting_sykepengegrunnlag_ref)
        VALUES (${sequence_number}, 1000, 1200, '${organisasjonsnummer}', ${sequence_number});
        
        INSERT INTO annullert_av_saksbehandler(id, annullert_tidspunkt, saksbehandler_ref, vedtaksperiode_id)
        VALUES (${sequence_number}, now(), '${saksbehandler_oid}', '${vedtaksperiode_id}');
        INSERT INTO ukoblede_annulleringer(id, annullert_tidspunkt, saksbehandler_ref)
        VALUES (${sequence_number}, now(), '${saksbehandler_oid}');
        INSERT INTO oppdrag(id, fagsystem_id, mottaker)
        VALUES (${sequence_number}, 'EN_PERSON_FAGSYSTEMID', 'MOTTAKER');
        INSERT INTO oppdrag(id, fagsystem_id, mottaker)
        VALUES (${sequence_number} + 1000, 'EN_ARBEIDSGIVER_FAGSYSTEMID', 'MOTTAKER');
        INSERT INTO utbetalingslinje(id, oppdrag_id, fom, tom, totalbeløp)
        VALUES (${sequence_number}, ${sequence_number}, '2018-01-01', '2018-01-31', 1000);
        INSERT INTO utbetalingslinje(id, oppdrag_id, fom, tom, totalbeløp)
        VALUES (${sequence_number} + 1000, ${sequence_number} + 1000, '2018-01-01', '2018-01-31', 1000);
        INSERT INTO utbetaling_id(id, utbetaling_id, person_ref, arbeidsgiver_identifikator, arbeidsgiver_fagsystem_id_ref,
                                  person_fagsystem_id_ref, type, opprettet, personbeløp, arbeidsgiverbeløp)
        VALUES (${sequence_number}, '${utbetaling_id}', ${sequence_number}, '${organisasjonsnummer}', ${sequence_number} + 1000,
                ${sequence_number}, 'UTBETALING', now(), 0, 0);
        
        INSERT INTO utbetaling(id, status, opprettet, data, utbetaling_id_ref)
        VALUES (${sequence_number}, 'UTBETALT', now(), '{}'::json, ${sequence_number});
        
        INSERT INTO totrinnsvurdering(id, vedtaksperiode_id, saksbehandler, beslutter, person_ref, tilstand, opprettet, oppdatert)
        VALUES (${sequence_number}, '${vedtaksperiode_id}', '${saksbehandler_oid}', '${saksbehandler_oid}', ${sequence_number}, 'AVVENTER_SAKSBEHANDLER', now(), null);
        
        INSERT INTO dialog(id, opprettet)
        VALUES (${periodehistorikk_dialog_id}, now());
        INSERT INTO pa_vent(id, vedtaksperiode_id, saksbehandler_ref, frist, opprettet, dialog_ref)
        VALUES (${sequence_number}, '${vedtaksperiode_id}', '${saksbehandler_oid}', now(), now(), ${periodehistorikk_dialog_id});
        INSERT INTO periodehistorikk(id, type, timestamp, generasjon_id, saksbehandler_oid, dialog_ref)
        VALUES (${sequence_number}, 'TOTRINNSVURDERING_RETUR', now(), '${generasjon_id}', '${saksbehandler_oid}',
                ${periodehistorikk_dialog_id});
        
        INSERT INTO oppgave_behandling_kobling(oppgave_id, behandling_id)
        VALUES (${sequence_number}, gen_random_uuid());
        
        INSERT INTO sammenligningsgrunnlag(id, fødselsnummer, skjæringstidspunkt, opprettet, sammenligningsgrunnlag)
        VALUES (${sequence_number}, ${fødselsnummer}, '2018-01-01', now(), '{}'::json);
        
        INSERT INTO avviksvurdering(id, unik_id, fødselsnummer, skjæringstidspunkt, opprettet, avviksprosent, beregningsgrunnlag, sammenligningsgrunnlag_ref)
        VALUES (${sequence_number}, '${avviksvurdering_unik_id}', ${fødselsnummer}, '2018-01-01', now(), 25.0, '{}'::json, ${sequence_number});
        
        INSERT INTO vilkarsgrunnlag_per_avviksvurdering(avviksvurdering_ref, vilkårsgrunnlag_id)
        VALUES ('${avviksvurdering_unik_id}', gen_random_uuid());
        
        INSERT INTO begrunnelse(id, tekst, type, saksbehandler_ref) VALUES(${sequence_number} + 420, 'avslagtekst', 'AVSLAG', '${saksbehandler_oid}');
        INSERT INTO vedtak_begrunnelse(vedtaksperiode_id, begrunnelse_ref, generasjon_ref) VALUES ('${vedtaksperiode_id}', ${sequence_number} + 420, ${sequence_number})
        """.trimIndent()
        sessionOf(dataSource).use { session ->
            session.update(queryOf(sql))
        }
    }

    protected fun assertTabellinnhold(
        comparison: Comparison,
        numRows: Int,
    ) {
        val tabeller = finnTabeller().toMutableList()
        tabeller.removeAll(
            listOf(
                "flyway_schema_history",
                "enhet",
                "global_snapshot_versjon",
                "saksbehandler",
                "arbeidsgiver",
                "api_varseldefinisjon",
                "saksbehandler_opptegnelse_sekvensnummer",
                "inntekt",
                "temp_manglende_varsler",
                "automatisering_korrigert_soknad",
                "spesialsak",
                "avviksvurdering_spinnvillgate",
                "vilkarsgrunnlag_per_avviksvurdering_spinnvillgate",
                "sammenligningsgrunnlag_spinnvillgate",
                "melding_duplikatkontroll",
                "poison_pill",
                "force_automatisering",
                "risikovurdering",
                "risikovurdering_arbeidsuforhetvurdering",
                "risikovurdering_faresignal",
                "overstyringer_for_vedtaksperioder",
                "tilkommen_inntekt_events",
                "ukoblede_annulleringer"
            ),
        )
        tabeller.forEach { tabellnavn ->
            val expectedRowCount =
                when (tabellnavn) {
                    in listOf("oppdrag", "utbetalingslinje", "dialog") -> numRows * 2
                    in listOf("begrunnelse") -> numRows * 4
                    else -> numRows
                }
            val rowCount = finnRowCount(tabellnavn)
            assertTrue(
                comparison.compare(rowCount, expectedRowCount),
            ) { "Table '$tabellnavn' has $rowCount row(s), expected it to be ${comparison.label} $expectedRowCount" }
        }
    }

    protected fun finnTabeller(): List<String> =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
            session.run(queryOf(query).map { it.string("table_name") }.asList)
        }

    private fun finnRowCount(tabellnavn: String): Int =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT COUNT(1) FROM $tabellnavn"
            session.run(queryOf(query).map { it.int(1) }.asSingle) ?: 0
        }

    @BeforeEach
    fun resetDatabase() {
        OppryddingDevDBTestFixture.fixture.truncate()
    }

    protected companion object {
        const val FØDSELSNUMMER = "12345678910"
    }
}


enum class Comparison(
    val label: String,
    val compare: (Int, Int) -> Boolean,
) {
    EXACTLY("exactly", { a, b -> a == b }),
    AT_LEAST("at least", { a, b -> a >= b }),
}

object OppryddingDevDBTestFixture {
    val fixture = ModuleIsolatedDBTestFixture("opprydding-dev")
}
