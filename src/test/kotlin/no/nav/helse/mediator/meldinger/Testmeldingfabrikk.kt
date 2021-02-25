package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.vedtak.SaksbehandlerInntektskilde
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random.Default.nextLong

internal class Testmeldingfabrikk(private val fødselsnummer: String, private val aktørId: String) {
    companion object {
        const val OSLO = "0301"
    }

    fun lagVedtaksperiodeEndret(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        forrigeTilstand: String = "FORRIGE_TILSTAND",
        gjeldendeTilstand: String = "GJELDENDE_TILSTAND"
    ) =
        nyHendelse(
            id, "vedtaksperiode_endret", mapOf(
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer,
                "gjeldendeTilstand" to gjeldendeTilstand,
                "forrigeTilstand" to forrigeTilstand
            )
        )

    fun lagVedtaksperiodeForkastet(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr"
    ) =
        nyHendelse(
            id, "vedtaksperiode_forkastet", mapOf(
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer
            )
        )

    fun lagGodkjenningsbehov(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        orgnummer: String = "orgnr",
        periodeFom: LocalDate = LocalDate.now(),
        periodeTom: LocalDate = LocalDate.now(),
        periodetype: Saksbehandleroppgavetype = Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING,
        fødselsnummer: String = this.fødselsnummer,
        aktørId: String = this.aktørId,
        inntektskilde: SaksbehandlerInntektskilde = SaksbehandlerInntektskilde.EN_ARBEIDSGIVER,
    ) =
        nyHendelse(
            id, "behov",
            mapOf(
                "@behov" to listOf("Godkjenning"),
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to orgnummer,
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "Godkjenning" to mapOf(
                    "periodeFom" to "$periodeFom",
                    "periodeTom" to "$periodeTom",
                    "periodetype" to periodetype.name,
                    "inntektskilde" to inntektskilde.name,
                    "aktiveVedtaksperioder" to listOf(
                        mapOf(
                            "orgnummer" to orgnummer,
                            "vedtaksperiodeId" to vedtaksperiodeId
                        )
                    )
                )
            )
        )

    fun lagArbeidsgiverinformasjonløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        orgnummer: String = "orgnr",
        navn: String,
        bransjer: List<String>
    ) = nyHendelse(
        id, "behov", mapOf(
            "@final" to true,
            "@behov" to listOf("Arbeidsgiverinformasjon"),
            "hendelseId" to "$hendelseId",
            "contextId" to "$contextId",
            "vedtaksperiodeId" to "$vedtaksperiodeId",
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "orgnummer" to orgnummer,
            "@løsning" to mapOf(
                "Arbeidsgiverinformasjon" to listOf(
                    mapOf(
                        "orgnummer" to orgnummer,
                        "navn" to navn,
                        "bransjer" to bransjer
                    )
                )
            )
        )
    )

    fun lagArbeidsforholdløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        løsning: List<Arbeidsforholdløsning.Løsning>
    ) = nyHendelse(
        id, "behov", mapOf(
            "@final" to true,
            "@behov" to listOf("Arbeidsforhold"),
            "hendelseId" to "$hendelseId",
            "contextId" to "$contextId",
            "vedtaksperiodeId" to "$vedtaksperiodeId",
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "orgnummer" to organisasjonsnummer,
            "@løsning" to mapOf("Arbeidsforhold" to løsning)
        )
    )

    fun lagPersoninfoløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        enhet: String = "0301"
    ) =
        nyHendelse(
            id, "behov", mapOf(
                "@final" to true,
                "@behov" to listOf("HentEnhet", "HentPersoninfo", "HentInfotrygdutbetalinger"),
                "hendelseId" to "$hendelseId",
                "contextId" to "$contextId",
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "orgnummer" to organisasjonsnummer,
                "HentInfotrygdutbetalinger" to mapOf(
                    "historikkFom" to "2017-01-01",
                    "historikkTom" to "2020-12-31"
                ),
                "@løsning" to mapOf(
                    "HentInfotrygdutbetalinger" to listOf(
                        mapOf(
                            "fom" to "2018-01-01",
                            "tom" to "2018-01-31",
                            "dagsats" to "1000.0",
                            "grad" to "100",
                            "typetekst" to "ArbRef",
                            "organisasjonsnummer" to organisasjonsnummer
                        )
                    ),
                    "HentEnhet" to enhet,
                    "HentPersoninfo" to mapOf(
                        "fornavn" to "Kari",
                        "mellomnavn" to "",
                        "etternavn" to "Nordmann",
                        "fødselsdato" to "1970-01-01",
                        "kjønn" to "Kvinne"
                    )
                )
            )
        )

    fun lagHentInfotrygdutbetalingerløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr"
    ) =
        nyHendelse(
            id, "behov", mapOf(
                "@final" to true,
                "@behov" to listOf("HentInfotrygdutbetalinger"),
                "hendelseId" to "$hendelseId",
                "contextId" to "$contextId",
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "orgnummer" to organisasjonsnummer,
                "HentInfotrygdutbetalinger" to mapOf(
                    "historikkFom" to "2017-01-01",
                    "historikkTom" to "2020-12-31"
                ),
                "@løsning" to mapOf(
                    "HentInfotrygdutbetalinger" to listOf(
                        mapOf(
                            "fom" to "2018-01-01",
                            "tom" to "2018-01-31",
                            "dagsats" to "1000.0",
                            "grad" to "100",
                            "typetekst" to "ArbRef",
                            "organisasjonsnummer" to organisasjonsnummer
                        )
                    )
                )
            )
        )

    fun lagHentPersoninfoløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr"
    ) =
        nyHendelse(
            id, "behov", mapOf(
                "@final" to true,
                "@behov" to listOf("HentPersoninfo"),
                "hendelseId" to "$hendelseId",
                "contextId" to "$contextId",
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "orgnummer" to organisasjonsnummer,
                "@løsning" to mapOf(
                    "HentPersoninfo" to mapOf(
                        "fornavn" to "Kari",
                        "mellomnavn" to "",
                        "etternavn" to "Nordmann",
                        "fødselsdato" to "1970-01-01",
                        "kjønn" to "Kvinne"
                    )
                )
            )
        )

    fun lagHentEnhetløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        enhet: String = OSLO
    ) =
        nyHendelse(
            id, "behov", mapOf(
                "@final" to true,
                "@behov" to listOf("HentEnhet"),
                "hendelseId" to "$hendelseId",
                "contextId" to contextId,
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "orgnummer" to organisasjonsnummer,
                "@løsning" to mapOf(
                    "HentEnhet" to enhet
                )
            )
        )

    fun lagSaksbehandlerløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        oppgaveId: Long = nextLong(),
        godkjent: Boolean = true,
        godkjenttidspunkt: LocalDateTime = LocalDateTime.now(),
        saksbehandlerident: String = "Z999999",
        saksbehandlerepost: String = "saksbehandler@nav.no",
        saksbehandlerOID: UUID = UUID.randomUUID(),
        årsak: String? = null,
        begrunnelser: List<String>? = null,
        kommentar: String? = null
    ) =
        nyHendelse(
            id, "saksbehandler_løsning", mutableMapOf<String, Any>(
                "fødselsnummer" to fødselsnummer,
                "hendelseId" to hendelseId,
                "contextId" to contextId,
                "oppgaveId" to oppgaveId,
                "godkjent" to godkjent,
                "godkjenttidspunkt" to godkjenttidspunkt,
                "saksbehandlerident" to saksbehandlerident,
                "saksbehandlerepost" to saksbehandlerepost,
                "saksbehandleroid" to saksbehandlerOID
            ).apply {
                årsak?.also { put("årsak", it) }
                begrunnelser?.also { put("begrunnelser", it) }
                kommentar?.also { put("kommentar", it) }
            }
        )

    fun lagDigitalKontaktinformasjonløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        erDigital: Boolean = true
    ): String =
        nyHendelse(
            id,
            "behov", mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "@final" to true,
                "@behov" to listOf("DigitalKontaktinformasjon"),
                "contextId" to contextId,
                "hendelseId" to hendelseId,
                "@løsning" to mapOf(
                    "DigitalKontaktinformasjon" to mapOf(
                        "erDigital" to erDigital
                    )
                )
            )
        )

    fun lagEgenAnsattløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        erEgenAnsatt: Boolean = false
    ): String = nyHendelse(
        id,
        "behov", mutableMapOf(
            "fødselsnummer" to fødselsnummer,
            "@final" to true,
            "@behov" to listOf("EgenAnsatt"),
            "contextId" to contextId,
            "hendelseId" to hendelseId,
            "@løsning" to mapOf(
                "EgenAnsatt" to erEgenAnsatt
            )
        )
    )

    fun lagÅpneGosysOppgaverløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        antall: Int = 0,
        oppslagFeilet: Boolean = false
    ): String =
        nyHendelse(
            id,
            "behov", mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "@final" to true,
                "@behov" to listOf("ÅpneOppgaver"),
                "contextId" to contextId,
                "hendelseId" to hendelseId,
                "@løsning" to mapOf(
                    "ÅpneOppgaver" to mapOf(
                        "antall" to antall,
                        "oppslagFeilet" to oppslagFeilet
                    )
                )
            )
        )

    fun lagRisikovurderingløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean = true,
        funn: JsonNode
    ): String =
        nyHendelse(
            id,
            "behov", mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "@final" to true,
                "@behov" to listOf("Risikovurdering"),
                "contextId" to contextId,
                "hendelseId" to hendelseId,
                "Risikovurdering" to mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeId.toString(),
                    "organisasjonsnummer" to "815493000",
                    "periodetype" to Saksbehandleroppgavetype.FORLENGELSE
                ),
                "@løsning" to mapOf(
                    "Risikovurdering" to mapOf(
                        "kanGodkjennesAutomatisk" to kanGodkjennesAutomatisk,
                        "funn" to funn,
                        "kontrollertOk" to emptyList<JsonNode>(),
                    )
                )
            )
        )

    fun lagOverstyring(
        id: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        dager: List<OverstyringDagDto> = emptyList(),
        begrunnelse: String = "begrunnelse",
        saksbehandlerOid: UUID = UUID.randomUUID(),
        saksbehandlerNavn: String = "saksbehandler",
        saksbehandlerEpost: String = "saksbehandler@nav.no"
    ) = nyHendelse(
        id, "overstyr_tidslinje", mapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "dager" to dager,
            "begrunnelse" to begrunnelse,
            "saksbehandlerOid" to saksbehandlerOid,
            "saksbehandlerNavn" to saksbehandlerNavn,
            "saksbehandlerEpost" to saksbehandlerEpost
        )
    )

    fun lagOppdragLinje(
        endringskode: String = "NY", // [NY, UENDR, ENDR]
        klassekode: String = "SPREFAG-IOP",
        statuskode: String = "OPPH",
        datoStatusFom: LocalDate = LocalDate.now(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        dagsats: Int = 111,
        lønn: Int = 111,
        grad: Double = 0.11,
        delytelseId: Int = 11,
        refDelytelseId: Int = 11,
        refFagsystemId: String = "refFagsystemId",
    ) =
        mapOf<String, Any>(
            "endringskode" to endringskode,
            "klassekode" to klassekode,
            "statuskode" to statuskode,
            "datoStatusFom" to datoStatusFom,
            "fom" to fom,
            "tom" to tom,
            "dagsats" to dagsats,
            "lønn" to lønn,
            "grad" to grad,
            "delytelseId" to delytelseId,
            "refDelytelseId" to refDelytelseId,
            "refFagsystemId" to refFagsystemId,
        )


    fun lagOppdrag(
        fagsystemId: String = "fagsystemId",
        fagområde: String = "SPREF",
        mottaker: String = "mottaker",
        endringskode: String = "ENDR",
        sisteArbeidsgiverdag: LocalDate = LocalDate.now(),
        linjer: List<Map<String, Any>> = listOf(lagOppdragLinje())
    ) =
        mapOf(
            "fagsystemId" to fagsystemId,
            "fagområde" to fagområde,
            "mottaker" to mottaker,
            "endringskode" to endringskode,
            "sisteArbeidsgiverdag" to sisteArbeidsgiverdag,
            "linjer" to linjer,
        )


    fun lagUtbelingEndret(
        id: UUID = UUID.randomUUID(),
        fødselsnummer: String = "12020052345",
        orgnummer: String = "123456789",
        utbetalingId: UUID = UUID.randomUUID(),
        type: String = "UTBETALING", // [UTBETALING, ANNULLERING, ETTERUTBETALING]
        status: String = "UTBETALT", // [IKKE_UTBETALT, FORKASTET, IKKE_GODKJENT, GODKJENT_UTEN_UTBETALING, GODKJENT, SENDT, OVERFØRT, UTBETALING_FEILET, UTBETALT, ANNULLERT]
        forrigeStatus: String = "IKKE_UTBETALT",
        opprettet: LocalDateTime = LocalDateTime.now(),
        arbeidsgiverOppdrag: Map<String, Any> = lagOppdrag(),
        personOppdrag: Map<String, Any> = lagOppdrag(),
    ) = nyHendelse(
        id, "utbetaling_endret", mapOf(
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to orgnummer,
            "utbetalingId" to utbetalingId,
            "type" to type,
            "gjeldendeStatus" to status,
            "forrigeStatus" to forrigeStatus,
            "@opprettet" to opprettet,
            "arbeidsgiverOppdrag" to arbeidsgiverOppdrag,
            "personOppdrag" to personOppdrag,
        )
    )

    fun lagAvbrytSaksbehandling(
        id: UUID = UUID.randomUUID(),
        fødselsnummer: String = "12020052345",
        vedtaksperiodeId: UUID
    ) = nyHendelse(
        id, "vedtaksperiode_reberegnet", mapOf(
            "vedtaksperiodeId" to vedtaksperiodeId,
            "fødselsnummer" to fødselsnummer
        )
    )

    private fun nyHendelse(id: UUID, navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(nyHendelse(id, navn) + hendelse).toJson()

    private fun nyHendelse(id: UUID, navn: String) = mutableMapOf(
        "@event_name" to navn,
        "@id" to id,
        "@opprettet" to LocalDateTime.now()
    )
}
