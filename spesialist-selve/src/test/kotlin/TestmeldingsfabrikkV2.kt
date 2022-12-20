import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.mediator.meldinger.Risikofunn.Companion.tilJson
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.IKKE_UTBETALT
import no.nav.helse.modell.utbetaling.Utbetalingsstatus.NY
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto

internal object TestmeldingsfabrikkV2 {
    fun lagSøknadSendt(
        organisasjonsnummer: String,
        aktørId: String,
        fødselsnummer: String,
        id: UUID,
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
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        forrigeTilstand: String = "AVVENTER_SIMULERING",
        gjeldendeTilstand: String = "AVVENTER_GODKJENNING",
        forårsaketAvId: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.of(2018, 1, 1),
        tom: LocalDate = LocalDate.of(2018, 1, 31),
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
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        id: UUID,
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
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        periodeFom: LocalDate = LocalDate.now(),
        periodeTom: LocalDate = LocalDate.now(),
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        skjæringstidspunkt: LocalDate = LocalDate.now(),
        førstegangsbehandling: Boolean = true,
        utbetalingtype: Utbetalingtype = UTBETALING,
        inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
        orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList(),
        id: UUID,
    ) =
        nyHendelse(
            id, "behov",
            mapOf(
                "@behov" to listOf("Godkjenning"),
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
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

    fun lagPersoninfoløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        id: UUID,
        hendelseId: UUID,
        contextId: UUID,
    ) = nyHendelse(
        id, "behov", mapOf(
            "@final" to true,
            "@behov" to listOf("HentPersoninfoV2"),
            "hendelseId" to "$hendelseId",
            "contextId" to contextId,
            "vedtaksperiodeId" to "$vedtaksperiodeId",
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "orgnummer" to organisasjonsnummer,
            "@løsning" to mapOf(
                "HentPersoninfoV2" to mapOf(
                    "ident" to fødselsnummer,
                    "fornavn" to "Kari",
                    "mellomnavn" to "",
                    "etternavn" to "Nordmann",
                    "fødselsdato" to "1970-01-01",
                    "kjønn" to "Kvinne",
                    "adressebeskyttelse" to "Ugradert"
                )
            )
        )
    )

    fun lagEnhetløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        id: UUID,
        hendelseId: UUID,
        contextId: UUID,
        enhet: String = "0301", // Oslo
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

    fun lagInfotrygdutbetalingerløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        id: UUID,
        hendelseId: UUID,
        contextId: UUID,
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

    fun lagArbeidsgiverinformasjonløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        ekstraArbeidsgivere: List<Testmeldingfabrikk.ArbeidsgiverinformasjonJson> = emptyList(),
        id: UUID,
        hendelseId: UUID,
        contextId: UUID,
    ) = nyHendelse(
        id, "behov", mapOf(
            "@final" to true,
            "@behov" to listOf("Arbeidsgiverinformasjon"),
            "hendelseId" to "$hendelseId",
            "contextId" to "$contextId",
            "vedtaksperiodeId" to "$vedtaksperiodeId",
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "orgnummer" to organisasjonsnummer,
            "@løsning" to mapOf(
                "Arbeidsgiverinformasjon" to ekstraArbeidsgivere.map(Testmeldingfabrikk.ArbeidsgiverinformasjonJson::toBody)
            )
        )
    )

    fun lagArbeidsforholdløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        løsning: List<Arbeidsforholdløsning.Løsning>,
        id: UUID,
        hendelseId: UUID,
        contextId: UUID,
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

    fun lagEgenAnsattløsning(
        aktørId: String,
        fødselsnummer: String,
        erEgenAnsatt: Boolean = false,
        id: UUID,
        hendelseId: UUID,
        contextId: UUID,
    ): String = nyHendelse(
        id,
        "behov", mutableMapOf(
            "aktørId" to aktørId,
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
        aktørId: String,
        fødselsnummer: String,
        vergemål: Testmeldingfabrikk.VergemålJson,
        id: UUID,
        hendelseId: UUID,
        contextId: UUID,
    ): String = nyHendelse(
        id,
        "behov", mutableMapOf(
            "aktørId" to aktørId,
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

    fun lagDigitalKontaktinformasjonløsning(
        aktørId: String,
        fødselsnummer: String,
        erDigital: Boolean = true,
        id: UUID,
        hendelseId: UUID,
        contextId: UUID,
    ): String =
        nyHendelse(
            id, "behov", mutableMapOf(
                "aktørId" to aktørId,
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

    fun lagInntektløsning(
        aktørId: String,
        fødselsnummer: String,
        id: UUID,
        hendelseId: UUID,
        contextId: UUID,
    ): String =
        nyHendelse(
            id, "behov", mutableMapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "@final" to true,
                "@behov" to listOf("InntekterForSykepengegrunnlag"),
                "contextId" to contextId,
                "hendelseId" to hendelseId,
                "@løsning" to mapOf(
                    "InntekterForSykepengegrunnlag" to listOf(
                        mapOf(
                            "årMåned" to YearMonth.now(),
                            "inntektsliste" to listOf(
                                mapOf(
                                    "beløp" to 20000,
                                    "inntektstype" to "LOENNSINNTEKT",
                                    "orgnummer" to "123456789"
                                )
                            )
                        )
                    )
                )
            )
        )

    fun lagÅpneGosysOppgaverløsning(
        aktørId: String,
        fødselsnummer: String,
        antall: Int = 0,
        oppslagFeilet: Boolean = false,
        id: UUID,
        hendelseId: UUID,
        contextId: UUID,
    ): String =
        nyHendelse(
            id,
            "behov", mutableMapOf(
                "aktørId" to aktørId,
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
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean = true,
        funn: List<Risikofunn>,
        id: UUID,
        hendelseId: UUID,
        contextId: UUID,
    ): String =
        nyHendelse(
            id, "behov", mutableMapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "@final" to true,
                "@behov" to listOf("Risikovurdering"),
                "contextId" to contextId,
                "hendelseId" to hendelseId,
                "Risikovurdering" to mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeId.toString(),
                    "organisasjonsnummer" to organisasjonsnummer,
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

    fun lagSaksbehandlerløsning(
        fødselsnummer: String,
        godkjent: Boolean = true,
        godkjenttidspunkt: LocalDateTime = LocalDateTime.now(),
        saksbehandlerident: String = "Z999999",
        saksbehandlerepost: String = "saksbehandler@nav.no",
        saksbehandlerOID: UUID = UUID.randomUUID(),
        årsak: String? = null,
        begrunnelser: List<String>? = null,
        kommentar: String? = null,
        id: UUID,
        hendelseId: UUID,
        oppgaveId: Long,
    ): String =
        nyHendelse(
            id, "saksbehandler_løsning", mutableMapOf<String, Any>(
                "fødselsnummer" to fødselsnummer,
                "hendelseId" to hendelseId,
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

    fun lagVedtakFattet(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        id: UUID,
    ): String = nyHendelse(
        id, "vedtak_fattet", mapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId
        )
    )


    fun lagGosysOppgaveEndret(
        aktørId: String,
        fødselsnummer: String,
        id: UUID,
    ): String =
        nyHendelse(
            id, "gosys_oppgave_endret", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
            )
        )

    fun lagOverstyringTidslinje(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        dager: List<OverstyringDagDto> = emptyList(),
        begrunnelse: String = "begrunnelse",
        saksbehandleroid: UUID = UUID.randomUUID(),
        saksbehandlernavn: String = "saksbehandler",
        saksbehandlerepost: String = "saksbehandler@nav.no",
        saksbehandlerident: String = "saksbehandlerIdent",
        id: UUID,
    ) = nyHendelse(
        id, "saksbehandler_overstyrer_tidslinje", mapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "dager" to dager,
            "begrunnelse" to begrunnelse,
            "saksbehandlerOid" to saksbehandleroid,
            "saksbehandlerIdent" to saksbehandlerident,
            "saksbehandlerNavn" to saksbehandlernavn,
            "saksbehandlerEpost" to saksbehandlerepost
        )
    )

    fun lagOverstyringInntekt(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        begrunnelse: String = "begrunnelse",
        månedligInntekt: Double = 25000.0,
        fraMånedligInntekt: Double = 25001.0,
        skjæringstidspunkt: LocalDate,
        forklaring: String = "forklaring",
        subsumsjon: Testmeldingfabrikk.SubsumsjonJson?,
        saksbehandleroid: UUID = UUID.randomUUID(),
        saksbehandlernavn: String = "saksbehandler",
        saksbehandlerepost: String = "saksbehandler@nav.no",
        saksbehandlerident: String = "saksbehandlerIdent",
        id: UUID,
    ) = nyHendelse(
        id, "saksbehandler_overstyrer_inntekt", mutableMapOf<String, Any>(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "begrunnelse" to begrunnelse,
            "forklaring" to forklaring,
            "saksbehandlerOid" to saksbehandleroid,
            "saksbehandlerIdent" to saksbehandlerident,
            "saksbehandlerNavn" to saksbehandlernavn,
            "saksbehandlerEpost" to saksbehandlerepost,
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
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate,
        overstyrteArbeidsforhold: List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>,
        saksbehandleroid: UUID = UUID.randomUUID(),
        saksbehandlernavn: String = "saksbehandler",
        saksbehandlerepost: String = "sara.saksbehandler@nav.no",
        saksbehandlerident: String = "saksbehandlerIdent",
        id: UUID,
    ) = nyHendelse(
        id, "saksbehandler_overstyrer_arbeidsforhold", mapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "saksbehandlerOid" to saksbehandleroid,
            "saksbehandlerIdent" to saksbehandlerident,
            "saksbehandlerNavn" to saksbehandlernavn,
            "saksbehandlerEpost" to saksbehandlerepost,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "overstyrteArbeidsforhold" to overstyrteArbeidsforhold
        )
    )

    fun lagUtbetalingEndret(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        type: String = "UTBETALING",
        forrigeStatus: Utbetalingsstatus = NY,
        gjeldendeStatus: Utbetalingsstatus = IKKE_UTBETALT,
        arbeidsgiverFagsystemId: String = "LWCBIQLHLJISGREBICOHAU",
        personFagsystemId: String = "ASJKLD90283JKLHAS3JKLF",
        id: UUID,
    ) = nyHendelse(
        id, "utbetaling_endret", mapOf(
            "utbetalingId" to "$utbetalingId",
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "type" to type,
            "forrigeStatus" to "$forrigeStatus",
            "gjeldendeStatus" to "$gjeldendeStatus",
            "arbeidsgiverOppdrag" to mapOf(
                "mottaker" to organisasjonsnummer,
                "fagområde" to "SPREF",
                "endringskode" to "NY",
                "fagsystemId" to arbeidsgiverFagsystemId,
                "nettoBeløp" to 20000,
                "sisteArbeidsgiverdag" to "${LocalDate.MIN}",
                "linjer" to listOf(
                    mapOf(
                        "fom" to "${LocalDate.now()}",
                        "tom" to "${LocalDate.now()}",
                        "dagsats" to 2000,
                        "totalbeløp" to 2000,
                        "lønn" to 2000,
                        "grad" to 100.00,
                        "refFagsystemId" to arbeidsgiverFagsystemId,
                        "delytelseId" to 2,
                        "refDelytelseId" to 1,
                        "datoStatusFom" to "${LocalDate.now()}",
                        "endringskode" to "NY",
                        "klassekode" to "SPREFAG-IOP",
                        "statuskode" to "OPPH"
                    ),
                    mapOf(
                        "fom" to "${LocalDate.now()}",
                        "tom" to "${LocalDate.now()}",
                        "dagsats" to 2000,
                        "totalbeløp" to 2000,
                        "lønn" to 2000,
                        "grad" to 100.00,
                        "refFagsystemId" to null,
                        "delytelseId" to 3,
                        "refDelytelseId" to null,
                        "datoStatusFom" to null,
                        "endringskode" to "NY",
                        "klassekode" to "SPREFAG-IOP",
                        "statuskode" to null
                    )
                )
            ),
            "personOppdrag" to mapOf(
                "mottaker" to fødselsnummer,
                "fagområde" to "SP",
                "endringskode" to "NY",
                "fagsystemId" to personFagsystemId,
                "nettoBeløp" to 0,
                "linjer" to emptyList<Map<String, Any>>()
            )
        )
    )

    fun lagVedtaksperiodeNyUtbetaling(
        id: UUID = UUID.randomUUID(),
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) =
        nyHendelse(
            id, "vedtaksperiode_ny_utbetaling", mapOf(
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "utbetalingId" to "$utbetalingId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer,
            )
        )

    fun lagAktivitetsloggNyAktivitet(
        id: UUID,
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        varselkoder: List<String>,
    ): String {
        return nyHendelse(
            id, "aktivitetslogg_ny_aktivitet",
            mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "aktiviteter" to varselkoder.map {
                    lagAktivitet(
                        fødselsnummer = fødselsnummer,
                        varselkode = it,
                        vedtaksperiodeId = vedtaksperiodeId,
                        organisasjonsnummer = organisasjonsnummer
                    )
                }
            )
        )
    }

    private fun lagAktivitet(
        id: UUID = UUID.randomUUID(),
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        varselkode: String,
    ): Map<String, Any> = mapOf(
        "id" to id,
        "melding" to "en melding",
        "nivå" to "VARSEL",
        "varselkode" to varselkode,
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
                    "organisasjonsnummer" to organisasjonsnummer
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

    fun lagUtbetalingAnnullert(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        epost: String,
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
        id: UUID,
    ) = nyHendelse(
        id, "utbetaling_annullert", mapOf(
            "utbetalingId" to "$utbetalingId",
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "epost" to epost,
            "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId,
            "personFagsystemId" to personFagsystemId,
            "tidspunkt" to LocalDateTime.now()
        )
    )


    private fun nyHendelse(id: UUID, navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(
            mutableMapOf<String, Any>(
                "@event_name" to navn,
                "@id" to id,
                "@opprettet" to LocalDateTime.now()
            ) + hendelse
        ).toJson()
}