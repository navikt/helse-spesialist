package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.meldinger.Risikofunn.Companion.tilJson
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import kotlin.random.Random.Default.nextLong

internal class Testmeldingfabrikk(private val fødselsnummer: String, private val aktørId: String) {
    companion object {
        const val OSLO = "0301"
    }

    fun lagAdressebeskyttelseEndret(
        id: UUID = UUID.randomUUID()
    ) = nyHendelse(
        id, "adressebeskyttelse_endret", mapOf(
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId
        )
    )

    fun lagSøknadSendt(
        id: UUID = UUID.randomUUID(),
        organisasjonsnummer: String,
        aktørId: String,
        fødselsnummer: String,
    ) =
        nyHendelse(
            id, "sendt_søknad_nav", mapOf(
                "fnr" to fødselsnummer,
                "aktorId" to aktørId,
                "arbeidsgiver" to mapOf(
                    "orgnummer" to organisasjonsnummer
                )
            )
        )

    fun lagVedtaksperiodeEndret(
        id: UUID = UUID.randomUUID(),
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String = "orgnr",
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        forrigeTilstand: String = "FORRIGE_TILSTAND",
        gjeldendeTilstand: String = "GJELDENDE_TILSTAND",
        forårsaketAvId: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.of(2018, 1, 1),
        tom: LocalDate = LocalDate.of(2018, 1, 31)
    ) =
        nyHendelse(
            id, "vedtaksperiode_endret", mapOf(
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer,
                "gjeldendeTilstand" to gjeldendeTilstand,
                "forrigeTilstand" to forrigeTilstand,
                "@forårsaket_av" to mapOf(
                    "id" to forårsaketAvId
                ),
                "fom" to fom,
                "tom" to tom
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
        utbetalingId: UUID = UUID.randomUUID(),
        orgnummer: String = "orgnr",
        periodeFom: LocalDate = now(),
        periodeTom: LocalDate = now(),
        skjæringstidspunkt: LocalDate = now(),
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        førstegangsbehandling: Boolean = true,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
        fødselsnummer: String = this.fødselsnummer,
        aktørId: String = this.aktørId,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList()
    ) =
        nyHendelse(
            id, "behov",
            mapOf(
                "@behov" to listOf("Godkjenning"),
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to orgnummer,
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "utbetalingId" to "$utbetalingId",
                "Godkjenning" to mapOf(
                    "periodeFom" to "$periodeFom",
                    "periodeTom" to "$periodeTom",
                    "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                    "periodetype" to periodetype.name,
                    "førstegangsbehandling" to førstegangsbehandling,
                    "utbetalingtype" to utbetalingtype.name,
                    "inntektskilde" to inntektskilde.name,
                    "orgnummereMedRelevanteArbeidsforhold" to orgnummereMedRelevanteArbeidsforhold
                )
            )
        )

    fun arbeidsgiverinformasjon(orgnummer: String, navn: String, bransjer: List<String>) =
        ArbeidsgiverinformasjonJson(orgnummer, navn, bransjer).toBody()

    fun arbeidsgiverinformasjon(
        orgnummer: String,
        navn: String,
        bransjer: List<String>,
        ekstraArbeidsgivere: List<ArbeidsgiverinformasjonJson>
    ) = (
            listOf(ArbeidsgiverinformasjonJson(orgnummer, navn, bransjer)) + ekstraArbeidsgivere
            ).map(ArbeidsgiverinformasjonJson::toBody)

    fun lagArbeidsgiverinformasjonløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        orgnummer: String = "orgnr",
        navn: String,
        bransjer: List<String>,
        ekstraArbeidsgivere: List<ArbeidsgiverinformasjonJson> = emptyList()
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
                "Arbeidsgiverinformasjon" to arbeidsgiverinformasjon(orgnummer, navn, bransjer, ekstraArbeidsgivere)
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

    fun lagHentPersoninfoV2(ident: String, adressebeskyttelse: String = "Ugradert") = mapOf(
        "ident" to ident,
        "fornavn" to "Kari",
        "mellomnavn" to "",
        "etternavn" to "Nordmann",
        "fødselsdato" to "1970-01-01",
        "kjønn" to "Kvinne",
        "adressebeskyttelse" to adressebeskyttelse
    )

    fun lagFullstendigBehov(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        behov: List<String>,
        detaljer: Map<String, Any>
    ) =
        nyHendelse(
            id, "behov", mapOf(
                "@final" to true,
                "@behov" to behov,
                "hendelseId" to "$hendelseId",
                "contextId" to "$contextId",
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "orgnummer" to organisasjonsnummer,
            ) + detaljer
        )

    fun lagPersoninfoløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        enhet: String = "0301",
        adressebeskyttelse: String = "Ugradert"
    ) = lagFullstendigBehov(
        id,
        hendelseId,
        contextId,
        vedtaksperiodeId,
        organisasjonsnummer,
        listOf("HentEnhet", "HentPersoninfoV2", "HentInfotrygdutbetalinger"),
        mapOf(
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
                "HentPersoninfoV2" to lagHentPersoninfoV2(fødselsnummer, adressebeskyttelse)
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

    fun lagUtbetalingAnnullertEvent(
        id: UUID = UUID.randomUUID(),
        fødselsnummer: String = "123456789",
        arbeidsgiverFagsystemId: UUID = UUID.randomUUID(),
        personFagsystemId: UUID? = null,
        utbetalingId: UUID = UUID.randomUUID(),
        annullertAvSaksbehandler: LocalDate = now(),
        saksbehandlerEpost: String = "saksbehandler_epost"
    ) =
        nyHendelse(
            id, "utbetaling_annullert", mapOf(
                "fødselsnummer" to fødselsnummer,
                "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId.toString(),
                "utbetalingId" to utbetalingId.toString(),
                "tidspunkt" to annullertAvSaksbehandler.toString(),
                "epost" to saksbehandlerEpost
            ).let { if (personFagsystemId != null) it.plus("personFagsystemId" to personFagsystemId) else it }
        )

    fun lagHentPersoninfoløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        adressebeskyttelse: String = "Ugradert"
    ) =
        nyHendelse(
            id, "behov", mapOf(
                "@final" to true,
                "@behov" to listOf("HentPersoninfoV2"),
                "hendelseId" to "$hendelseId",
                "contextId" to "$contextId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "@løsning" to mapOf(
                    "HentPersoninfoV2" to mapOf(
                        "fornavn" to "Kari",
                        "mellomnavn" to "",
                        "etternavn" to "Nordmann",
                        "fødselsdato" to "1970-01-01",
                        "kjønn" to "Kvinne",
                        "adressebeskyttelse" to adressebeskyttelse
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

    fun lagVedtakFattet(
        id: UUID = UUID.randomUUID(),
        fødselsnummer: String = this.fødselsnummer,
        vedtaksperiodeId: UUID
    ): String = nyHendelse(
        id, "vedtak_fattet", mapOf(
            "fødselsnummer" to fødselsnummer,
            "vedtaksperiodeId" to "$vedtaksperiodeId"
        )
    )

    fun lagEgenAnsattløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        erEgenAnsatt: Boolean = false,
        fødselsnummer: String = this.fødselsnummer,
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

    fun lagVergemålløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vergemål: VergemålJson
    ): String = nyHendelse(
        id,
        "behov", mutableMapOf(
            "fødselsnummer" to fødselsnummer,
            "@final" to true,
            "@behov" to listOf("Vergemål"),
            "contextId" to contextId,
            "hendelseId" to hendelseId,
            "@løsning" to mapOf(
                "Vergemål" to vergemål.toBody()
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
        funn: List<Risikofunn>
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
                    "periodetype" to Periodetype.FORLENGELSE
                ),
                "@løsning" to mapOf(
                    "Risikovurdering" to mapOf(
                        "kanGodkjennesAutomatisk" to kanGodkjennesAutomatisk,
                        "funn" to funn.tilJson(),
                        "kontrollertOk" to emptyList<JsonNode>(),
                    )
                )
            )
        )

    fun lagOverstyringTidslinje(
        id: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        dager: List<OverstyringDagDto> = emptyList(),
        begrunnelse: String = "begrunnelse",
        saksbehandlerOid: UUID = UUID.randomUUID(),
        saksbehandlerNavn: String = "saksbehandler",
        saksbehandlerEpost: String = "saksbehandler@nav.no",
        saksbehandlerident: String = "saksbehandlerIdent"
    ) = nyHendelse(
        id, "saksbehandler_overstyrer_tidslinje", mapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "dager" to dager,
            "begrunnelse" to begrunnelse,
            "saksbehandlerOid" to saksbehandlerOid,
            "saksbehandlerIdent" to saksbehandlerident,
            "saksbehandlerNavn" to saksbehandlerNavn,
            "saksbehandlerEpost" to saksbehandlerEpost
        )
    )

    fun lagOverstyringInntekt(
        id: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        begrunnelse: String = "begrunnelse",
        forklaring: String = "forklaring",
        saksbehandlerOid: UUID = UUID.randomUUID(),
        saksbehandlerNavn: String = "saksbehandler",
        saksbehandlerEpost: String = "saksbehandler@nav.no",
        saksbehandlerident: String = "saksbehandlerIdent",
        månedligInntekt: Double = 25000.0,
        fraMånedligInntekt: Double = 25001.0,
        skjæringstidspunkt: LocalDate,
        subsumsjon: SubsumsjonJson?
    ) = nyHendelse(
        id, "saksbehandler_overstyrer_inntekt", mutableMapOf<String, Any>(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "begrunnelse" to begrunnelse,
            "forklaring" to forklaring,
            "saksbehandlerOid" to saksbehandlerOid,
            "saksbehandlerIdent" to saksbehandlerident,
            "saksbehandlerNavn" to saksbehandlerNavn,
            "saksbehandlerEpost" to saksbehandlerEpost,
            "månedligInntekt" to månedligInntekt,
            "fraMånedligInntekt" to fraMånedligInntekt,
            "skjæringstidspunkt" to skjæringstidspunkt,
        ).apply {
            subsumsjon?.let {
                this["subsumsjon"] = mutableMapOf(
                    "paragraf" to subsumsjon.paragraf
                ).apply {
                    subsumsjon.ledd?.let { ledd ->
                        this["ledd"] = ledd
                    }
                    subsumsjon.bokstav?.let { bokstav ->
                        this["bokstav"] = bokstav
                    }
                }
            }
        }
    )

    fun lagOverstyringArbeidsforhold(
        id: UUID = UUID.randomUUID(),
        overstyrteArbeidsforhold: List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>,
        organisasjonsnummer: String = "orgnr",
        saksbehandlerOid: UUID = UUID.randomUUID(),
        saksbehandlerNavn: String = "saksbehandler",
        saksbehandlerEpost: String = "sara.saksbehandler@nav.no",
        saksbehandlerident: String = "saksbehandlerIdent",
        skjæringstidspunkt: LocalDate
    ) = nyHendelse(
        id, "saksbehandler_overstyrer_arbeidsforhold", mapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "saksbehandlerOid" to saksbehandlerOid,
            "saksbehandlerIdent" to saksbehandlerident,
            "saksbehandlerNavn" to saksbehandlerNavn,
            "saksbehandlerEpost" to saksbehandlerEpost,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "overstyrteArbeidsforhold" to overstyrteArbeidsforhold
        )
    )

    fun lagOppdragLinje(
        endringskode: String = "NY", // [NY, UENDR, ENDR]
        klassekode: String = "SPREFAG-IOP",
        statuskode: String = "OPPH",
        datoStatusFom: LocalDate = now(),
        fom: LocalDate = now(),
        tom: LocalDate = now(),
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
        sisteArbeidsgiverdag: LocalDate = now(),
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


    fun lagUtbetalingEndret(
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

    fun lagRevurderingAvvist(
        id: UUID = UUID.randomUUID(),
        fødselsnummer: String,
        errors: List<String>
    ) = nyHendelse(
        id, "revurdering_avvist", mapOf(
            "fødselsnummer" to fødselsnummer,
            "errors" to errors
        )
    )

    fun lagNyeVarsler(
        id: UUID,
        vedtaksperiodeId: UUID,
        orgnummer: String,
        aktiviteter: List<Map<String, Any>> =
            listOf(
                lagAktivitet(
                    orgnummer = orgnummer,
                    vedtaksperiodeId = vedtaksperiodeId
                )
            )

    ): String {
        return nyHendelse(
            id, "aktivitetslogg_ny_aktivitet",
            mapOf(
                "fødselsnummer" to fødselsnummer,
                "aktiviteter" to aktiviteter
            )
        )
    }

    fun lagAktivitet(
        id: UUID = UUID.randomUUID(),
        kode: String = "RV_VV",
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        orgnummer: String,
    ): Map<String, Any> = mapOf(
        "id" to id,
        "melding" to "en melding",
        "nivå" to "VARSEL",
        "varselkode" to kode,
        "tidsstempel" to LocalDateTime.now(),
        "kontekster" to listOf(
            mapOf(
                "konteksttype" to "Person",
                "kontekstmap" to mapOf(
                    "fødselsnummer" to fødselsnummer,
                    "aktørId" to "2093088099680"
                )
            ),
            mapOf(
                "konteksttype" to "Arbeidsgiver",
                "kontekstmap" to mapOf(
                    "organisasjonsnummer" to orgnummer
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

    internal fun nyHendelse(id: UUID, navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(nyHendelse(id, navn) + hendelse).toJson()

    private fun nyHendelse(id: UUID, navn: String) = mutableMapOf(
        "@event_name" to navn,
        "@id" to id,
        "@opprettet" to LocalDateTime.now()
    )


    data class SubsumsjonJson(
        val paragraf: String,
        val ledd: String?,
        val bokstav: String?
    )

    data class ArbeidsgiverinformasjonJson(
        private val orgnummer: String,
        private val navn: String,
        private val bransjer: List<String>
    ) {
        fun toBody() = mapOf(
            "orgnummer" to orgnummer,
            "navn" to navn,
            "bransjer" to bransjer
        )
    }

    data class VergemålJson(
        val vergemål: List<Vergemål> = emptyList(),
        val fremtidsfullmakter: List<Vergemål> = emptyList(),
        val fullmakter: List<Fullmakt> = emptyList()
    ) {
        fun toBody() = mapOf(
            "vergemål" to vergemål,
            "fremtidsfullmakter" to fremtidsfullmakter,
            "fullmakter" to fullmakter,
        )

        data class Vergemål(
            val type: VergemålType
        )

        data class Fullmakt(
            val områder: List<Område>,
            val gyldigFraOgMed: LocalDate,
            val gyldigTilOgMed: LocalDate
        )

        @Suppress("unused")
        enum class Område { Alle, Syk, Sym, Annet }

        @Suppress("unused")
        enum class VergemålType {
            ensligMindreaarigAsylsoeker,
            ensligMindreaarigFlyktning,
            voksen,
            midlertidigForVoksen,
            mindreaarig,
            midlertidigForMindreaarig,
            forvaltningUtenforVergemaal,
            stadfestetFremtidsfullmakt
        }
    }
}
