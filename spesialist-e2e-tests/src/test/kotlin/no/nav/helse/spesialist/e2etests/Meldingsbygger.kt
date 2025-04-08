package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.e2etests.context.Arbeidsgiver
import no.nav.helse.spesialist.e2etests.context.Person
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object Meldingsbygger {
    fun byggSendSøknadNav(person: Person, arbeidsgiver: Arbeidsgiver) =
        JsonMessage.newMessage(
            mapOf(
                "@event_name" to "sendt_søknad_nav",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "fnr" to person.fødselsnummer,
                "aktorId" to person.aktørId,
                "arbeidsgiver" to mapOf(
                    "orgnummer" to arbeidsgiver.organisasjonsnummer
                )
            )
        ).toJson()

    fun byggBehandlingOpprettet(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "behandling_opprettet",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "vedtaksperiodeId" to vedtaksperiode.vedtaksperiodeId,
            "behandlingId" to vedtaksperiode.spleisBehandlingIdForÅByggeMelding("behandling_opprettet"),
            "fødselsnummer" to person.fødselsnummer,
            "aktørId" to person.aktørId,
            "organisasjonsnummer" to arbeidsgiver.organisasjonsnummer,
            "fom" to vedtaksperiode.fom,
            "tom" to vedtaksperiode.tom,
        )
    ).toJson()

    fun byggVedtaksperiodeNyUtbetaling(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "vedtaksperiode_ny_utbetaling",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "vedtaksperiodeId" to vedtaksperiode.vedtaksperiodeId,
            "utbetalingId" to vedtaksperiode.utbetalingIdForÅByggeMelding("vedtaksperiode_ny_utbetaling"),
            "fødselsnummer" to person.fødselsnummer,
            "aktørId" to person.aktørId,
            "organisasjonsnummer" to arbeidsgiver.organisasjonsnummer,
        )
    ).toJson()

    fun byggVedtaksperiodeEndret(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "vedtaksperiode_endret",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "vedtaksperiodeId" to vedtaksperiode.vedtaksperiodeId,
            "fødselsnummer" to person.fødselsnummer,
            "forrigeTilstand" to "AVVENTER_GODKJENNING",
            "gjeldendeTilstand" to "AVVENTER_BLOKKERENDE_PERIODE",
        )
    ).toJson()

    fun byggGodkjenningsbehov(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vilkårsgrunnlagId: UUID,
        vedtaksperiode: Vedtaksperiode
    ): String {
        val meldingsnavn = "Godkjenningsbehov"
        return JsonMessage.newMessage(
            mapOf(
                "@event_name" to "behov",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "@behov" to listOf("Godkjenning"),
                "aktørId" to person.aktørId,
                "fødselsnummer" to person.fødselsnummer,
                "organisasjonsnummer" to arbeidsgiver.organisasjonsnummer,
                "vedtaksperiodeId" to vedtaksperiode.vedtaksperiodeId,
                "utbetalingId" to vedtaksperiode.utbetalingIdForÅByggeMelding(meldingsnavn),
                "Godkjenning" to mapOf(
                    "periodeFom" to vedtaksperiode.fom,
                    "periodeTom" to vedtaksperiode.tom,
                    "skjæringstidspunkt" to vedtaksperiode.fom,
                    "periodetype" to "FØRSTEGANGSBEHANDLING",
                    "førstegangsbehandling" to true,
                    "utbetalingtype" to "UTBETALING",
                    "inntektskilde" to "EN_ARBEIDSGIVER",
                    "orgnummereMedRelevanteArbeidsforhold" to emptyList<String>(),
                    "kanAvvises" to true,
                    "vilkårsgrunnlagId" to vilkårsgrunnlagId,
                    "behandlingId" to vedtaksperiode.spleisBehandlingIdForÅByggeMelding(meldingsnavn),
                    "tags" to listOf("Innvilget"),
                    "perioderMedSammeSkjæringstidspunkt" to listOf(
                        mapOf(
                            "fom" to vedtaksperiode.fom,
                            "tom" to vedtaksperiode.tom,
                            "vedtaksperiodeId" to vedtaksperiode.vedtaksperiodeId,
                            "behandlingId" to vedtaksperiode.spleisBehandlingIdForÅByggeMelding(meldingsnavn)
                        )
                    ),
                    "sykepengegrunnlagsfakta" to mapOf(
                        "fastsatt" to "EtterHovedregel",
                        "arbeidsgivere" to listOf(
                            mapOf(
                                "arbeidsgiver" to arbeidsgiver.organisasjonsnummer,
                                "omregnetÅrsinntekt" to 123456.7,
                                "inntektskilde" to "Arbeidsgiver",
                            )
                        )
                    ),
                    "omregnedeÅrsinntekter" to listOf(
                        mapOf(
                            "organisasjonsnummer" to arbeidsgiver.organisasjonsnummer,
                            "beløp" to 123456.7,
                        )
                    ),
                ),
            )
        ).toJson()
    }

    fun byggGosysOppgaveEndret(person: Person) =
        JsonMessage.newMessage(
            mapOf(
                "@event_name" to "gosys_oppgave_endret",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "fødselsnummer" to person.fødselsnummer
            )
        ).toJson()

    fun byggAktivitetsloggNyAktivitetMedVarsler(
        varselkoder: List<String>,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vedtaksperiode: Vedtaksperiode
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "aktivitetslogg_ny_aktivitet",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "aktørId" to person.aktørId,
            "fødselsnummer" to person.fødselsnummer,
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
                                "fødselsnummer" to person.fødselsnummer,
                                "aktørId" to person.aktørId
                            )
                        ),
                        mapOf(
                            "konteksttype" to "Arbeidsgiver",
                            "kontekstmap" to mapOf(
                                "organisasjonsnummer" to arbeidsgiver.organisasjonsnummer
                            )
                        ),
                        mapOf(
                            "konteksttype" to "Vedtaksperiode",
                            "kontekstmap" to mapOf(
                                "vedtaksperiodeId" to vedtaksperiode.vedtaksperiodeId
                            )
                        )
                    )
                )
            }
        )
    ).toJson()

    fun byggUtbetalingEndret(
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        forrigeStatus: String,
        gjeldendeStatus: String
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "utbetaling_endret",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "utbetalingId" to vedtaksperiode.utbetalingIdForÅByggeMelding("utbetaling_endret"),
            "aktørId" to person.aktørId,
            "fødselsnummer" to person.fødselsnummer,
            "organisasjonsnummer" to arbeidsgiver.organisasjonsnummer,
            "type" to "UTBETALING",
            "forrigeStatus" to forrigeStatus,
            "gjeldendeStatus" to gjeldendeStatus,
            "@opprettet" to LocalDateTime.now(),
            "arbeidsgiverOppdrag" to mapOf(
                "mottaker" to arbeidsgiver.organisasjonsnummer,
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
                "mottaker" to person.fødselsnummer,
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

    fun byggAvsluttetMedVedtak(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vedtaksperiode: Vedtaksperiode
    ) = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "avsluttet_med_vedtak",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "aktørId" to person.aktørId,
            "fødselsnummer" to person.fødselsnummer,
            "organisasjonsnummer" to arbeidsgiver.organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiode.vedtaksperiodeId,
            "behandlingId" to vedtaksperiode.spleisBehandlingIdForÅByggeMelding("avsluttet_med_vedtak"),
            "fom" to (1 jan 2018),
            "tom" to (31 jan 2018),
            "skjæringstidspunkt" to (1 jan 2018),
            "sykepengegrunnlag" to 600000.0,
            "vedtakFattetTidspunkt" to LocalDateTime.now(),
            "hendelser" to emptyList<String>(),
            "utbetalingId" to vedtaksperiode.utbetalingIdForÅByggeMelding("avsluttet_med_vedtak"),
            "sykepengegrunnlagsfakta" to mapOf(
                "fastsatt" to "EtterHovedregel",
                "omregnetÅrsinntekt" to 600000.0,
                "6G" to 6 * 118620.0,
                "arbeidsgivere" to listOf(
                    mapOf(
                        "arbeidsgiver" to arbeidsgiver.organisasjonsnummer,
                        "omregnetÅrsinntekt" to 600000.00,
                    )
                ),
                "innrapportertÅrsinntekt" to 600000.0,
                "avviksprosent" to 0,
            ),
        )
    ).toJson()

    fun byggVarselkodeNyDefinisjon(varselkode: String) =
        JsonMessage.newMessage(
            mapOf(
                "@event_name" to "varselkode_ny_definisjon",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "varselkode" to varselkode,
                "gjeldende_definisjon" to mapOf(
                    "id" to UUID.randomUUID(),
                    "kode" to varselkode,
                    "tittel" to "En tittel for varselkode=$varselkode",
                    "forklaring" to "En forklaring for varselkode=$varselkode",
                    "handling" to "En handling for varselkode=$varselkode",
                    "avviklet" to false,
                    "opprettet" to LocalDateTime.now()
                )
            )
        ).toJson()

    fun byggAdressebeskyttelseEndret(person: Person) =
        JsonMessage.newMessage(
            mapOf(
                "@event_name" to "adressebeskyttelse_endret",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "fødselsnummer" to person.fødselsnummer,
                "aktørId" to person.aktørId
            )
        ).toJson()

    fun byggEndretSkjermetinfo(person: Person, skjermet: Boolean) =
        JsonMessage.newMessage(
            mapOf(
                "@event_name" to "endret_skjermetinfo",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "fødselsnummer" to person.fødselsnummer,
                "skjermet" to skjermet,
            )
        ).toJson()
}
