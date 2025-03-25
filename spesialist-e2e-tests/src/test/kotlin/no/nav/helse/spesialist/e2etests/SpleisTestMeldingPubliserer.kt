package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.test.TestPerson
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SpleisTestMeldingPubliserer(
    private val testPerson: TestPerson,
    private val vedtaksperiodeId: UUID,
    private val rapidsConnection: RapidsConnection
) {
    val spleisBehandlingId: UUID = UUID.randomUUID()

    fun simulerPublisertSendtSøknadNavMelding() {
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "sendt_søknad_nav",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "fnr" to testPerson.fødselsnummer,
                    "aktorId" to testPerson.aktørId,
                    "arbeidsgiver" to mapOf(
                        "orgnummer" to testPerson.orgnummer
                    )
                )
            ).toJson()
        )
    }

    fun simulerPublisertBehandlingOpprettetMelding() {
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "behandling_opprettet",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "behandlingId" to spleisBehandlingId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "aktørId" to testPerson.aktørId,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "fom" to (1 jan 2018),
                    "tom" to (31 jan 2018)
                )
            ).toJson()
        )
    }

    fun simulerPublisertGodkjenningsbehovMelding() {
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "behov",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "@behov" to listOf("Godkjenning"),
                    "aktørId" to testPerson.aktørId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "utbetalingId" to testPerson.utbetalingId1,
                    "Godkjenning" to mapOf(
                        "periodeFom" to (1 jan 2018),
                        "periodeTom" to (31 jan 2018),
                        "skjæringstidspunkt" to (1 jan 2018),
                        "periodetype" to "FØRSTEGANGSBEHANDLING",
                        "førstegangsbehandling" to true,
                        "utbetalingtype" to "UTBETALING",
                        "inntektskilde" to "EN_ARBEIDSGIVER",
                        "orgnummereMedRelevanteArbeidsforhold" to emptyList<String>(),
                        "kanAvvises" to true,
                        "vilkårsgrunnlagId" to UUID.randomUUID(),
                        "behandlingId" to spleisBehandlingId,
                        "tags" to listOf("Innvilget"),
                        "perioderMedSammeSkjæringstidspunkt" to listOf(
                            mapOf(
                                "fom" to (1 jan 2018),
                                "tom" to (31 jan 2018),
                                "vedtaksperiodeId" to vedtaksperiodeId,
                                "behandlingId" to spleisBehandlingId
                            )
                        ),
                        "sykepengegrunnlagsfakta" to mapOf(
                            "fastsatt" to "EtterHovedregel",
                            "arbeidsgivere" to listOf(
                                mapOf(
                                    "arbeidsgiver" to testPerson.orgnummer,
                                    "omregnetÅrsinntekt" to 123456.7,
                                    "inntektskilde" to "Arbeidsgiver",
                                )
                            )
                        ),
                        "omregnedeÅrsinntekter" to listOf(
                            mapOf(
                                "organisasjonsnummer" to testPerson.orgnummer,
                                "beløp" to 123456.7,
                            )
                        ),
                    ),
                )
            ).toJson()
        )
    }

    fun simulerPublisertGosysOppgaveEndretMelding() {
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "gosys_oppgave_endret",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "fødselsnummer" to testPerson.fødselsnummer
                )
            ).toJson()
        )
    }

    fun simulerPublisertVedtaksperiodeEndretMelding() {
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "vedtaksperiode_endret",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "aktørId" to testPerson.aktørId,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "gjeldendeTilstand" to "AVVENTER_GODKJENNING",
                    "forrigeTilstand" to "AVVENTER_SIMULERING",
                    "@forårsaket_av" to mapOf(
                        "id" to UUID.randomUUID()
                    ),
                    "fom" to (1 jan 2018),
                    "tom" to (31 jan 2018)
                )
            ).toJson()
        )
    }

    fun simulerPublisertVedtaksperiodeNyUtbetalingMelding() {
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "vedtaksperiode_ny_utbetaling",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "vedtaksperiodeId" to vedtaksperiodeId,
                    "utbetalingId" to testPerson.utbetalingId1,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "aktørId" to testPerson.aktørId,
                    "organisasjonsnummer" to testPerson.orgnummer,
                )
            ).toJson()
        )
    }

    fun simulerPublisertUtbetalingEndretMelding() {
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "utbetaling_endret",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "utbetalingId" to testPerson.utbetalingId1,
                    "aktørId" to testPerson.aktørId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "type" to "UTBETALING",
                    "forrigeStatus" to "NY",
                    "gjeldendeStatus" to "IKKE_UTBETALT",
                    "@opprettet" to LocalDateTime.now(),
                    "arbeidsgiverOppdrag" to mapOf(
                        "mottaker" to testPerson.orgnummer,
                        "fagområde" to "SPREF",
                        "fagsystemId" to "LWCBIQLHLJISGREBICOHAU",
                        "nettoBeløp" to 20000,
                        "linjer" to listOf(
                            mapOf(
                                "fom" to LocalDate.now(),
                                "tom" to LocalDate.now(),
                                "totalbeløp" to 2000
                            ),
                            mapOf(
                                "fom" to LocalDate.now(),
                                "tom" to LocalDate.now(),
                                "totalbeløp" to 2000
                            )
                        )
                    ),
                    "personOppdrag" to mapOf(
                        "mottaker" to testPerson.fødselsnummer,
                        "fagområde" to "SP",
                        "fagsystemId" to "ASJKLD90283JKLHAS3JKLF",
                        "nettoBeløp" to 0,
                        "linjer" to listOf(
                            mapOf(
                                "fom" to LocalDate.now(),
                                "tom" to LocalDate.now(),
                                "totalbeløp" to 2000
                            ),
                            mapOf(
                                "fom" to LocalDate.now(),
                                "tom" to LocalDate.now(),
                                "totalbeløp" to 2000
                            )
                        )

                    )
                )
            ).toJson()
        )
    }

    fun simulerPublisertAktivitetsloggNyAktivitetMelding(varselkoder: List<String>) {
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "aktivitetslogg_ny_aktivitet",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "aktørId" to testPerson.aktørId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "aktiviteter" to varselkoder.map { varselkode ->
                        mapOf(
                            "id" to UUID.randomUUID(),
                            "melding" to "en melding",
                            "nivå" to "VARSEL",
                            "varselkode" to varselkode,
                            "tidsstempel" to LocalDateTime.now(),
                            "kontekster" to listOf(
                                mapOf(
                                    "konteksttype" to "Person",
                                    "kontekstmap" to mapOf(
                                        "fødselsnummer" to testPerson.fødselsnummer,
                                        "aktørId" to testPerson.aktørId
                                    )
                                ),
                                mapOf(
                                    "konteksttype" to "Arbeidsgiver",
                                    "kontekstmap" to mapOf(
                                        "organisasjonsnummer" to testPerson.orgnummer
                                    )
                                ),
                                mapOf(
                                    "konteksttype" to "Vedtaksperiode",
                                    "kontekstmap" to mapOf(
                                        "vedtaksperiodeId" to vedtaksperiodeId
                                    )
                                )
                            )
                        )
                    }
                )
            ).toJson()
        )
    }

    fun håndterUtbetalingUtbetalt() {
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "utbetaling_endret",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "utbetalingId" to testPerson.utbetalingId1,
                    "aktørId" to testPerson.aktørId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "type" to "UTBETALING",
                    "forrigeStatus" to "SENDT",
                    "gjeldendeStatus" to "UTBETALT",
                    "@opprettet" to LocalDateTime.now(),
                    "arbeidsgiverOppdrag" to mapOf(
                        "mottaker" to testPerson.orgnummer,
                        "fagområde" to "SPREF",
                        "fagsystemId" to "LWCBIQLHLJISGREBICOHAU",
                        "nettoBeløp" to 20000,
                        "linjer" to listOf(
                            mapOf(
                                "fom" to "${LocalDate.now()}",
                                "tom" to "${LocalDate.now()}",
                                "totalbeløp" to 2000
                            ),
                            mapOf(
                                "fom" to "${LocalDate.now()}",
                                "tom" to "${LocalDate.now()}",
                                "totalbeløp" to 2000
                            )
                        )
                    ),
                    "personOppdrag" to mapOf(
                        "mottaker" to testPerson.fødselsnummer,
                        "fagområde" to "SP",
                        "fagsystemId" to "ASJKLD90283JKLHAS3JKLF",
                        "nettoBeløp" to 0,
                        "linjer" to listOf(
                            mapOf(
                                "fom" to "${LocalDate.now()}",
                                "tom" to "${LocalDate.now()}",
                                "totalbeløp" to 2000
                            ),
                            mapOf(
                                "fom" to "${LocalDate.now()}",
                                "tom" to "${LocalDate.now()}",
                                "totalbeløp" to 2000
                            )
                        )

                    )
                )
            ).toJson()
        )
    }

    fun håndterAvsluttetMedVedtak() {
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mapOf(
                    "@event_name" to "avsluttet_med_vedtak",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "aktørId" to testPerson.aktørId,
                    "fødselsnummer" to testPerson.fødselsnummer,
                    "organisasjonsnummer" to testPerson.orgnummer,
                    "vedtaksperiodeId" to this.vedtaksperiodeId,
                    "behandlingId" to spleisBehandlingId,
                    "fom" to (1 jan 2018),
                    "tom" to (31 jan 2018),
                    "skjæringstidspunkt" to (1 jan 2018),
                    "sykepengegrunnlag" to 600000.0,
                    "grunnlagForSykepengegrunnlag" to 600000.0,
                    "grunnlagForSykepengegrunnlagPerArbeidsgiver" to emptyMap<String, Double>(),
                    "begrensning" to "VET_IKKE",
                    "inntekt" to 600000.0,
                    "vedtakFattetTidspunkt" to LocalDateTime.now(),
                    "hendelser" to emptyList<String>(),
                    "utbetalingId" to testPerson.utbetalingId1,
                    "sykepengegrunnlagsfakta" to mapOf(
                        "fastsatt" to "EtterHovedregel",
                        "omregnetÅrsinntekt" to 600000.0,
                        "6G" to 6 * 118620.0,
                        "arbeidsgivere" to listOf(
                            mapOf(
                                "arbeidsgiver" to testPerson.orgnummer,
                                "omregnetÅrsinntekt" to 600000.00,
                            )
                        ),
                        "innrapportertÅrsinntekt" to 600000.0,
                        "avviksprosent" to 0,
                    ),
                )
            ).toJson()
        )
    }
}