package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.AvviksvurderingTestdata
import no.nav.helse.Testdata
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.Risikofunn.Companion.tilJson
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.saksbehandler.OverstyrtInntektOgRefusjonEvent
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vilkårsprøving.LovhjemmelEvent
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import kotlin.random.Random.Default.nextLong

internal object Testmeldingfabrikk {
    private const val OSLO = "0301"

    fun lagVedtaksperiodeNyUtbetaling(
        aktørId: String,
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        organisasjonsnummer: String,
        id: UUID = UUID.randomUUID(),
    ) = nyHendelse(
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
                        kode = it,
                        vedtaksperiodeId = vedtaksperiodeId,
                        orgnummer = organisasjonsnummer,
                    )
                }
            )
        )
    }

    fun lagGosysOppgaveEndret(
        fødselsnummer: String,
        id: UUID,
    ): String =
        nyHendelse(id, "gosys_oppgave_endret", mapOf("fødselsnummer" to fødselsnummer))

    fun lagEndretSkjermetinfo(
        fødselsnummer: String,
        skjermet: Boolean,
        id: UUID,
    ): String = nyHendelse(
        id, "endret_skjermetinfo", mapOf(
            "fødselsnummer" to fødselsnummer,
            "skjermet" to skjermet,
            "@opprettet" to LocalDateTime.now()
        )
    )

    fun lagAdressebeskyttelseEndret(
        aktørId: Any,
        fødselsnummer: Any,
        id: UUID = UUID.randomUUID(),
    ) = nyHendelse(
        id, "adressebeskyttelse_endret", mapOf(
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId
        )
    )

    fun lagSøknadSendt(
        organisasjonsnummer: String,
        aktørId: String,
        fødselsnummer: String,
        id: UUID = UUID.randomUUID(),
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

    fun lagVedtaksperiodeOpprettet(
        id: UUID = UUID.randomUUID(),
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String = "orgnr",
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        forårsaketAvId: UUID = UUID.randomUUID(),
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = 1.januar,
    ) =
        nyHendelse(
            id, "vedtaksperiode_opprettet", mapOf(
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer,
                "@forårsaket_av" to mapOf(
                    "id" to forårsaketAvId,
                    "opprettet" to 1.februar.atStartOfDay()
                ),
                "fom" to fom,
                "tom" to tom,
                "skjæringstidspunkt" to skjæringstidspunkt
            )
        )

    fun lagSaksbehandlerSkjønnsfastsettingSykepengegrunnlag(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        skjæringstidspunkt: LocalDate = 1.januar,
        saksbehandlerOid: UUID,
        saksbehandlerEpost: String,
        saksbehandlerIdent: String,
        saksbehandlerNavn: String,
        id: UUID,
    ) =
        nyHendelse(
            id, "saksbehandler_skjonnsfastsetter_sykepengegrunnlag", mapOf(
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "arbeidsgivere" to listOf(
                    mapOf(
                        "organisasjonsnummer" to organisasjonsnummer,
                        "årlig" to 600000.0,
                        "fraÅrlig" to 500000.0,
                        "årsak" to "Skjønnsfastsetting ved mer enn 25% avvik",
                        "type" to "OMREGNET_ÅRSINNTEKT",
                        "begrunnelseMal" to "Mal",
                        "begrunnelseFritekst" to "Fritekst",
                        "begrunnelseKonklusjon" to "Fritekst",
                        "subsumsjon" to mapOf(
                            "paragraf" to "8-30",
                            "ledd" to "2",
                            "bokstav" to null,
                        ),
                        "initierendeVedtaksperiodeId" to vedtaksperiodeId,
                    )
                ),
                "skjæringstidspunkt" to skjæringstidspunkt,
                "saksbehandlerIdent" to saksbehandlerIdent,
                "saksbehandlerOid" to saksbehandlerOid,
                "saksbehandlerNavn" to saksbehandlerNavn,
                "saksbehandlerEpost" to saksbehandlerEpost,
            )
        )

    fun lagVedtaksperiodeForkastet(
        aktørId: String,
        fødselsnummer: String,
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        id: UUID = UUID.randomUUID(),
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
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        periodeFom: LocalDate = now(),
        periodeTom: LocalDate = now(),
        skjæringstidspunkt: LocalDate = now(),
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        førstegangsbehandling: Boolean = true,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList(),
        kanAvvises: Boolean = true,
        id: UUID = UUID.randomUUID(),
        vilkårsgrunnlagId: UUID = UUID.randomUUID(),
        avviksvurderingId: UUID = UUID.randomUUID(),
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
                    "orgnummereMedRelevanteArbeidsforhold" to orgnummereMedRelevanteArbeidsforhold,
                    "kanAvvises" to kanAvvises,
                    "vilkårsgrunnlagId" to vilkårsgrunnlagId
                ),
                "avviksvurderingId" to avviksvurderingId
            )
        )

    private fun arbeidsgiverinformasjon(ekstraArbeidsgivere: List<ArbeidsgiverinformasjonJson>) =
        ekstraArbeidsgivere.map(ArbeidsgiverinformasjonJson::toBody)

    fun lagArbeidsgiverinformasjonløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        ekstraArbeidsgivere: List<ArbeidsgiverinformasjonJson> = emptyList(),
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
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
                "Arbeidsgiverinformasjon" to arbeidsgiverinformasjon(ekstraArbeidsgivere)
            )
        )
    )

    fun lagArbeidsgiverinformasjonKomposittLøsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        organisasjoner: List<ArbeidsgiverinformasjonJson> = emptyList(),
        personer: List<Map<String, Any>> = emptyList(),
        hendelseId: UUID,
        contextId: UUID,
        id: UUID,
    ) = nyHendelse(
        id, "behov", mapOf(
            "@final" to true,
            "@behov" to listOf("Arbeidsgiverinformasjon", "HentPersoninfoV2"),
            "hendelseId" to "$hendelseId",
            "contextId" to "$contextId",
            "vedtaksperiodeId" to "$vedtaksperiodeId",
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "orgnummer" to organisasjonsnummer,
            "@løsning" to mapOf(
                "Arbeidsgiverinformasjon" to organisasjoner.map(ArbeidsgiverinformasjonJson::toBody),
                "HentPersoninfoV2" to personer
            )
        )
    )

    fun lagArbeidsforholdløsning(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        løsning: List<Arbeidsforholdløsning.Løsning>,
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
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

    private fun lagHentPersoninfoV2(ident: String, adressebeskyttelse: String = "Ugradert") = mapOf(
        "ident" to ident,
        "fornavn" to "Kari",
        "mellomnavn" to "",
        "etternavn" to "Nordmann",
        "fødselsdato" to "1970-01-01",
        "kjønn" to "Kvinne",
        "adressebeskyttelse" to adressebeskyttelse
    )

    private fun lagFullstendigBehov(
        aktørId: String,
        fødselsnummer: String,
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        behov: List<String>,
        detaljer: Map<String, Any>,
        id: UUID = UUID.randomUUID(),
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

    fun lagPersoninfoløsningComposite(
        aktørId: String,
        fødselsnummer: String,
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        enhet: String = "0301",
        adressebeskyttelse: String = "Ugradert",
        id: UUID = UUID.randomUUID(),
    ) = lagFullstendigBehov(
        aktørId,
        fødselsnummer,
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
        ),
        id
    )

    fun lagInfotrygdutbetalingerløsning(
        aktørId: String,
        fødselsnummer: String,
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        id: UUID = UUID.randomUUID(),
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

    fun lagUtbetalingAnnullert(
        fødselsnummer: String = "123456789",
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String? = null,
        utbetalingId: UUID = UUID.randomUUID(),
        annullertAvSaksbehandler: LocalDateTime = LocalDateTime.now(),
        saksbehandlerEpost: String = "saksbehandler_epost",
        id: UUID = UUID.randomUUID(),
    ) =
        nyHendelse(
            id, "utbetaling_annullert", mapOf(
                "fødselsnummer" to fødselsnummer,
                "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId,
                "utbetalingId" to utbetalingId.toString(),
                "tidspunkt" to annullertAvSaksbehandler.toString(),
                "epost" to saksbehandlerEpost
            ).let { if (personFagsystemId != null) it.plus("personFagsystemId" to personFagsystemId) else it }
        )

    fun lagSykefraværstilfeller(
        fødselsnummer: String,
        aktørId: String,
        tilfeller: List<Map<String, Any>>,
        id: UUID = UUID.randomUUID(),
    ) = nyHendelse(
        id, "sykefraværstilfeller", mapOf(
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "tilfeller" to tilfeller,
        )
    )

    fun lagPersoninfoløsning(
        aktørId: String,
        fødselsnummer: String,
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        adressebeskyttelse: String = "Ugradert",
        id: UUID = UUID.randomUUID(),
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

    fun lagEnhetløsning(
        aktørId: String,
        fødselsnummer: String,
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        enhet: String = OSLO,
        id: UUID = UUID.randomUUID(),
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

    fun lagOverstyringIgangsatt(
        fødselsnummer: String,
        berørtePerioder: List<Map<String, String>> = listOf(
            mapOf(
                "vedtaksperiodeId" to "${UUID.randomUUID()}",
                "skjæringstidspunkt" to "2022-01-01",
                "periodeFom" to "2022-01-01",
                "periodeTom" to "2022-01-31",
                "orgnummer" to "orgnr",
                "typeEndring" to "REVURDERING"
            )
        ),
        årsak: String = "KORRIGERT_SØKNAD",
        fom: LocalDate = now(),
        kilde: UUID = UUID.randomUUID(),
        id: UUID = UUID.randomUUID(),
    ) =
        nyHendelse(
            id, "overstyring_igangsatt", mapOf(
                "fødselsnummer" to fødselsnummer,
                "årsak" to årsak, // Denne leses rett fra hendelse-tabellen i HendelseDao, ikke via riveren
                "berørtePerioder" to berørtePerioder,
                "kilde" to "$kilde",
                "periodeForEndringFom" to "$fom",
            )
        )

    fun lagHentDokumentLøsning(
        fødselsnummer: String,
        dokumentId: UUID,
        id: UUID = UUID.randomUUID(),
        dokument: JsonNode = objectMapper.createObjectNode(),
    ) =
        nyHendelse(
            id, "hent-dokument", mapOf(
                "fødselsnummer" to fødselsnummer,
                "dokumentId" to "$dokumentId",
                "@løsning" to mapOf("dokument" to dokument),
            )
        )

    fun lagOverstyringInntektOgRefusjon(
        aktørId: String,
        fødselsnummer: String,
        arbeidsgivere: List<OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiverEvent> = listOf(
            OverstyrtInntektOgRefusjonEvent.OverstyrtArbeidsgiverEvent(
                organisasjonsnummer = Testdata.ORGNR,
                månedligInntekt = 25000.0,
                fraMånedligInntekt = 25001.0,
                forklaring = "testbortforklaring",
                subsumsjon = LovhjemmelEvent("8-28", "LEDD_1", "BOKSTAV_A", "folketrygdloven", "1970-01-01"),
                refusjonsopplysninger = null,
                fraRefusjonsopplysninger = null,
                begrunnelse = "en begrunnelse"
            )
        ),
        skjæringstidspunkt: LocalDate,
        saksbehandleroid: UUID = UUID.randomUUID(),
        saksbehandlernavn: String = "saksbehandler",
        saksbehandlerepost: String = "saksbehandler@nav.no",
        saksbehandlerident: String = "saksbehandlerIdent",
        id: UUID = UUID.randomUUID(),
    ) = nyHendelse(
        id, "saksbehandler_overstyrer_inntekt_og_refusjon", mapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "arbeidsgivere" to arbeidsgivere,
            "saksbehandlerOid" to saksbehandleroid,
            "saksbehandlerIdent" to saksbehandlerident,
            "saksbehandlerNavn" to saksbehandlernavn,
            "saksbehandlerEpost" to saksbehandlerepost,
            "skjæringstidspunkt" to skjæringstidspunkt,
        )
    )

    fun lagOverstyringArbeidsforhold(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate,
        overstyrteArbeidsforhold: List<OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdFraApi>,
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
        forrigeStatus: Utbetalingsstatus = Utbetalingsstatus.NY,
        gjeldendeStatus: Utbetalingsstatus = Utbetalingsstatus.IKKE_UTBETALT,
        arbeidsgiverFagsystemId: String = "LWCBIQLHLJISGREBICOHAU",
        personFagsystemId: String = "ASJKLD90283JKLHAS3JKLF",
        arbeidsgiverbeløp: Int = 20000,
        personbeløp: Int = 0,
        opprettet: LocalDateTime,
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
            "@opprettet" to opprettet,
            "arbeidsgiverOppdrag" to mapOf(
                "mottaker" to organisasjonsnummer,
                "fagområde" to "SPREF",
                "fagsystemId" to arbeidsgiverFagsystemId,
                "nettoBeløp" to arbeidsgiverbeløp,
                "linjer" to listOf(
                    mapOf(
                        "fom" to "${now()}",
                        "tom" to "${now()}",
                        "totalbeløp" to 2000
                    ),
                    mapOf(
                        "fom" to "${now()}",
                        "tom" to "${now()}",
                        "totalbeløp" to 2000
                    )
                )
            ),
            "personOppdrag" to mapOf(
                "mottaker" to fødselsnummer,
                "fagområde" to "SP",
                "fagsystemId" to personFagsystemId,
                "nettoBeløp" to personbeløp,
                "linjer" to listOf(
                    mapOf(
                        "fom" to "${now()}",
                        "tom" to "${now()}",
                        "totalbeløp" to 2000
                    ),
                    mapOf(
                        "fom" to "${now()}",
                        "tom" to "${now()}",
                        "totalbeløp" to 2000
                    )
                )

            )
        )
    )

    fun lagOppdaterPersonsnapshot(aktørId: String, fødselsnummer: String, id: UUID) = nyHendelse(
        id, "oppdater_personsnapshot", mapOf(
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId
        )
    )

    fun lagSaksbehandlerløsning(
        fødselsnummer: String,
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        oppgaveId: Long = nextLong(),
        godkjent: Boolean = true,
        godkjenttidspunkt: LocalDateTime = LocalDateTime.now(),
        saksbehandlerident: String = "Z999999",
        saksbehandlerepost: String = "saksbehandler@nav.no",
        saksbehandlerOID: UUID = UUID.randomUUID(),
        saksbehandleroverstyringer: List<UUID> = emptyList(),
        årsak: String? = null,
        begrunnelser: List<String>? = null,
        kommentar: String? = null,
        id: UUID = UUID.randomUUID(),
    ) =
        nyHendelse(
            id, "saksbehandler_løsning", mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "hendelseId" to hendelseId,
                "contextId" to contextId,
                "behandlingId" to UUID.randomUUID(),
                "oppgaveId" to oppgaveId,
                "godkjent" to godkjent,
                "godkjenttidspunkt" to godkjenttidspunkt,
                "saksbehandlerident" to saksbehandlerident,
                "saksbehandlerepost" to saksbehandlerepost,
                "saksbehandleroid" to saksbehandlerOID,
                "saksbehandleroverstyringer" to saksbehandleroverstyringer,
            ).apply {
                årsak?.also { put("årsak", it) }
                begrunnelser?.also { put("begrunnelser", it) }
                kommentar?.also { put("kommentar", it) }
            }
        )

    fun lagInntektløsning(
        aktørId: String,
        fødselsnummer: String,
        orgnummer: String,
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
    ): String =
        nyHendelse(
            id,
            "behov", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "@final" to true,
                "@behov" to listOf("InntekterForSykepengegrunnlag"),
                "contextId" to contextId,
                "hendelseId" to hendelseId,
                "orgnummer" to orgnummer,
                "@løsning" to mapOf(
                    "InntekterForSykepengegrunnlag" to listOf(
                        mapOf(
                            "årMåned" to "${YearMonth.now().minusMonths(1)}",
                            "inntektsliste" to listOf(
                                mapOf(
                                    "beløp" to 20000,
                                    "inntektstype" to "LOENNSINNTEKT",
                                    "orgnummer" to orgnummer
                                )
                            )
                        )
                    )
                )
            )
        )

    fun lagEgenAnsattløsning(
        aktørId: String,
        fødselsnummer: String,
        erEgenAnsatt: Boolean = false,
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
    ): String = nyHendelse(
        id,
        "behov", mapOf(
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
        vergemål: VergemålJson,
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
    ): String = nyHendelse(
        id,
        "behov", mapOf(
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

    fun lagÅpneGosysOppgaverløsning(
        aktørId: String,
        fødselsnummer: String,
        antall: Int = 0,
        oppslagFeilet: Boolean = false,
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
    ): String =
        nyHendelse(
            id,
            "behov", mapOf(
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
        organisasjonsnummer: String = Testdata.ORGNR,
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean = true,
        funn: List<Risikofunn>,
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
    ): String =
        nyHendelse(
            id,
            "behov", mapOf(
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "@final" to true,
                "@behov" to listOf("Risikovurdering"),
                "contextId" to contextId,
                "hendelseId" to hendelseId,
                "Risikovurdering" to mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeId.toString(),
                    "organisasjonsnummer" to organisasjonsnummer,
                    "førstegangsbehandling" to Periodetype.FORLENGELSE,
                    "kunRefusjon" to true,
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


    fun lagNyeVarsler(
        fødselsnummer: String,
        id: UUID,
        vedtaksperiodeId: UUID,
        orgnummer: String,
        aktiviteter: List<Map<String, Any>> =
            listOf(
                lagAktivitet(
                    fødselsnummer = fødselsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    orgnummer = orgnummer,
                )
            ),

        ): String {
        return nyHendelse(
            id, "aktivitetslogg_ny_aktivitet",
            mapOf(
                "fødselsnummer" to fødselsnummer,
                "aktiviteter" to aktiviteter
            )
        )
    }

    private fun lagAktivitet(
        fødselsnummer: String,
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

    fun lagUtkastTilVedtak(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID?,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        fastsattType: String,
        id: UUID,
        inkluderSpleisverdier: Boolean = true,
    ): String = nyHendelse(
        id, "utkast_til_vedtak", mutableMapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "fom" to fom,
            "tom" to tom,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "sykepengegrunnlag" to 600000.0,
            "grunnlagForSykepengegrunnlag" to 600000.0,
            "grunnlagForSykepengegrunnlagPerArbeidsgiver" to emptyMap<String, Double>(),
            "begrensning" to "VET_IKKE",
            "inntekt" to 600000.0,
            "vedtakFattetTidspunkt" to LocalDateTime.now(),
            "hendelser" to emptyList<String>(),
            "tags" to emptyList<String>()
        ).apply {
            compute("utbetalingId") { _, _ -> utbetalingId }
            if (utbetalingId != null) {
                val sykepengegrunnlagsfakta = when (fastsattType) {
                    "EtterSkjønn" -> fastsattEtterSkjønn(organisasjonsnummer, inkluderSpleisverdier)
                    "EtterHovedregel" -> fastsattEtterHovedregel(organisasjonsnummer, inkluderSpleisverdier)
                    "IInfotrygd" -> fastsattIInfotrygd()
                    else -> throw IllegalArgumentException("$fastsattType er ikke en gyldig fastsatt-type")
                }
                put("sykepengegrunnlagsfakta", sykepengegrunnlagsfakta)
            }
        }
    )

    private fun fastsattEtterSkjønn(
        organisasjonsnummer: String,
        inkluderSpleisverdier: Boolean = true,
    ): Map<String, Any> {
        return mutableMapOf(
            "fastsatt" to "EtterSkjønn",
            "omregnetÅrsinntekt" to 500000.0,
            "skjønnsfastsatt" to 600000.0,
            "6G" to 6 * 118620.0,
            "tags" to emptyList<String>(),
            "arbeidsgivere" to listOf(
                mapOf(
                    "arbeidsgiver" to organisasjonsnummer,
                    "omregnetÅrsinntekt" to 500000.00,
                    "skjønnsfastsatt" to 600000.00
                )
            )
        ).apply {
            if (inkluderSpleisverdier) {
                this["innrapportertÅrsinntekt"] = 600000.0
                this["avviksprosent"] = 16.67
            }
        }

    }

    private fun fastsattEtterHovedregel(
        organisasjonsnummer: String,
        inkluderSpleisverdier: Boolean = true,
    ): Map<String, Any> {
        return mutableMapOf(
            "fastsatt" to "EtterHovedregel",
            "omregnetÅrsinntekt" to 600000.0,
            "6G" to 6 * 118620.0,
            "tags" to emptyList<String>(),
            "arbeidsgivere" to listOf(
                mapOf(
                    "arbeidsgiver" to organisasjonsnummer,
                    "omregnetÅrsinntekt" to 600000.00,
                )
            )
        ).apply {
            if (inkluderSpleisverdier) {
                this["innrapportertÅrsinntekt"] = 600000.0
                this["avviksprosent"] = 0
            }
        }
    }

    private fun fastsattIInfotrygd(): Map<String, Any> {
        return mapOf(
            "fastsatt" to "IInfotrygd",
            "omregnetÅrsinntekt" to 500000.0,
        )
    }

    private fun nyHendelse(id: UUID, navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(nyHendelse(id, navn) + hendelse).toJson()

    private fun nyHendelse(id: UUID, navn: String) = mutableMapOf(
        "@event_name" to navn,
        "@id" to id,
        "@opprettet" to LocalDateTime.now()
    )

    fun lagAvviksvurdering(avviksvurderingTestdata: AvviksvurderingTestdata, id: UUID): String =
        nyHendelse(
            id, "avvik_vurdert", mapOf(
                "fødselsnummer" to avviksvurderingTestdata.fødselsnummer,
                "aktørId" to avviksvurderingTestdata.aktørId,
                "skjæringstidspunkt" to avviksvurderingTestdata.skjæringstidspunkt,
                "avviksvurdering" to mapOf(
                    "id" to avviksvurderingTestdata.avviksvurderingId,
                    "opprettet" to 1.januar.atStartOfDay(),
                    "beregningsgrunnlag" to mapOf(
                        "totalbeløp" to 550000.0, "omregnedeÅrsinntekter" to listOf(
                            mapOf(
                                "arbeidsgiverreferanse" to avviksvurderingTestdata.organisasjonsnummer,
                                "beløp" to 250000.0
                            ),
                        )
                    ),
                    "sammenligningsgrunnlag" to mapOf(
                        "id" to "887b2e4c-5222-45f1-9831-1846a028193b",
                        "totalbeløp" to avviksvurderingTestdata.sammenligningsgrunnlag,
                        "innrapporterteInntekter" to listOf(
                            mapOf(
                                "arbeidsgiverreferanse" to avviksvurderingTestdata.organisasjonsnummer,
                                "inntekter" to listOf(
                                    mapOf(
                                        "årMåned" to YearMonth.from(1.januar),
                                        "beløp" to 10000.0
                                    ),
                                )
                            )
                        )
                    ),
                    "avviksprosent" to avviksvurderingTestdata.avviksprosent,
                )
            )
        )

    data class ArbeidsgiverinformasjonJson(
        private val orgnummer: String,
        private val navn: String,
        private val bransjer: List<String>,
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
        val fullmakter: List<Fullmakt> = emptyList(),
    ) {
        fun toBody() = mapOf(
            "vergemål" to vergemål,
            "fremtidsfullmakter" to fremtidsfullmakter,
            "fullmakter" to fullmakter,
        )

        data class Vergemål(
            val type: VergemålType,
        )

        data class Fullmakt(
            val områder: List<Område>,
            val gyldigFraOgMed: LocalDate,
            val gyldigTilOgMed: LocalDate,
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
