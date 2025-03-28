package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.mediator.meldinger.Risikofunn.Companion.tilJson
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.util.januar
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random.Default.nextLong

object Testmeldingfabrikk {
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
                    lagVarsel(
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

    fun lagTilbakedateringBehandlet(
        fødselsnummer: String,
        id: UUID,
        perioder: List<Periode>,
    ): String =
        nyHendelse(
            id = id,
            navn = "tilbakedatering_behandlet",
            hendelse = mapOf(
                "fødselsnummer" to fødselsnummer,
                "sykmeldingId" to "${UUID.randomUUID()}",
                "perioder" to perioder.map {
                    mapOf(
                        "fom" to it.fom,
                        "tom" to it.tom
                    )
                }
            )
        )

    fun lagFullmaktløsningMedFullmakt(
        fødselsnummer: String,
        fom: LocalDate,
        tom: LocalDate?
    ): String = nyHendelse(
        id = UUID.randomUUID(), "behov",
        mapOf(
            "fødselsnummer" to fødselsnummer,
            "@final" to true,
            "@behov" to listOf("Fullmakt"),
            "contextId" to "${UUID.randomUUID()}",
            "hendelseId" to "${UUID.randomUUID()}",
            "@løsning" to mapOf(
                "Fullmakt" to listOf(
                    mapOf(
                        "omraade" to listOf("SYK", "SYM"),
                        "gyldigFraOgMed" to fom,
                        "gyldigTilOgMed" to tom,
                    )
                )

            )
        )
    )

    fun lagFullmaktløsningUtenFullmakter(
        fødselsnummer: String,
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
    ): String = nyHendelse(
        id = UUID.randomUUID(), "behov",
        mapOf(
            "fødselsnummer" to fødselsnummer,
            "@final" to true,
            "@behov" to listOf("Fullmakt"),
            "contextId" to "$contextId",
            "hendelseId" to "$hendelseId",
            "@løsning" to mapOf(
                "Fullmakt" to emptyList<Map<String, String>>()
            )
        )
    )

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

    fun lagBehandlingOpprettet(
        id: UUID = UUID.randomUUID(),
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID,
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
    ) =
        nyHendelse(
            id, "behandling_opprettet", mapOf(
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "behandlingId" to "$spleisBehandlingId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer,
                "fom" to fom,
                "tom" to tom
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
        spleisBehandlingId: UUID = UUID.randomUUID(),
        tags: List<String> = emptyList(),
        fastsatt: String = "EtterHovedregel",
        skjønnsfastsatt: Double? = null,
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
                    "vilkårsgrunnlagId" to vilkårsgrunnlagId,
                    "behandlingId" to spleisBehandlingId,
                    "tags" to tags,
                    "perioderMedSammeSkjæringstidspunkt" to listOf(
                        mapOf(
                            "fom" to "$periodeFom",
                            "tom" to "$periodeTom",
                            "vedtaksperiodeId" to "$vedtaksperiodeId",
                            "behandlingId" to "$spleisBehandlingId"
                        )
                    ),
                    "sykepengegrunnlagsfakta" to mapOf(
                        "fastsatt" to fastsatt,
                        "arbeidsgivere" to listOf(
                            mutableMapOf(
                                "arbeidsgiver" to organisasjonsnummer,
                                "omregnetÅrsinntekt" to 123456.7,
                                "inntektskilde" to "Arbeidsgiver",
                            ).apply {
                                if (skjønnsfastsatt != null) {
                                    put("skjønnsfastsatt", skjønnsfastsatt)
                                }
                            }
                        )
                    ),
                    "omregnedeÅrsinntekter" to listOf(
                        mapOf(
                            "organisasjonsnummer" to organisasjonsnummer,
                            "beløp" to 123456.7,
                        )
                    ),
                ),
            )
        )

    fun lagSaksbehandlerløsning(
        fødselsnummer: String
    ) =
        nyHendelse(
            UUID.randomUUID(), "saksbehandler_løsning",
            mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "oppgaveId" to 3333333,
                "hendelseId" to UUID.randomUUID(),
                "behandlingId" to UUID.randomUUID(),
                "godkjent" to true,
                "saksbehandlerident" to "X001122",
                "saksbehandleroid" to UUID.randomUUID(),
                "saksbehandlerepost" to "en.saksbehandler@nav.no",
                "godkjenttidspunkt" to "2024-07-27T08:05:22.051807803",
                "saksbehandleroverstyringer" to emptyList<String>(),
                "saksbehandler" to mapOf(
                    "ident" to "X001122",
                    "epostadresse" to "en.saksbehandler@nav.no"
                )
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


    fun lagVergemålOgFullmaktKomposittLøsning(
        aktørId: String,
        fødselsnummer: String,
        vergemål: VergemålJson,
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
    ) = nyHendelse(
        id, "behov", mapOf(
            "@final" to true,
            "@behov" to listOf("Vergemål", "Fullmakt"),
            "hendelseId" to "$hendelseId",
            "contextId" to "$contextId",
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "@løsning" to mapOf(
                "Vergemål" to vergemål.toBody(),
                "Fullmakt" to emptyList<Map<String, String>>()
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

    fun lagAvviksvurderingløsning(
        fødselsnummer: String,
        organisasjonsnummer: String,
        sammenligningsgrunnlagTotalbeløp: Double,
        avviksprosent: Double,
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        id: UUID = UUID.randomUUID(),
        avviksvurderingId: UUID = UUID.randomUUID(),
    ) =
        nyHendelse(
            id, "behov", mapOf(
                "@final" to true,
                "@behov" to listOf("Avviksvurdering"),
                "hendelseId" to "$hendelseId",
                "contextId" to "$contextId",
                "fødselsnummer" to fødselsnummer,
                "@løsning" to mapOf(
                    "Avviksvurdering" to mapOf(
                        "avviksvurderingId" to avviksvurderingId,
                        "utfall" to "NyVurderingForetatt",
                        "avviksprosent" to avviksprosent,
                        "harAkseptabeltAvvik" to true,
                        "maksimaltTillattAvvik" to 25.0,
                        "opprettet" to LocalDateTime.now(),
                        "beregningsgrunnlag" to mapOf(
                            "totalbeløp" to 600000.0,
                            "omregnedeÅrsinntekter" to listOf(
                                mapOf(
                                    "arbeidsgiverreferanse" to organisasjonsnummer,
                                    "beløp" to 600000.0
                                )
                            )
                        ),
                        "sammenligningsgrunnlag" to mapOf(
                            "totalbeløp" to sammenligningsgrunnlagTotalbeløp,
                            "innrapporterteInntekter" to listOf(
                                mapOf(
                                    "arbeidsgiverreferanse" to organisasjonsnummer,
                                    "inntekter" to listOf(
                                        mapOf(
                                            "årMåned" to YearMonth.now(),
                                            "beløp" to sammenligningsgrunnlagTotalbeløp
                                        )
                                    )
                                )
                            )
                        ),
                    )
                )
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

    fun lagOppdaterPersondata(aktørId: String, fødselsnummer: String, id: UUID) = nyHendelse(
        id, "oppdater_persondata", mutableMapOf(
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId
        )
    )

    fun lagKlargjørPersonForVisning(aktørId: String, fødselsnummer: String, id: UUID) = nyHendelse(
        id, "klargjør_person_for_visning", mutableMapOf(
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
        beslutterident: String? = null,
        beslutterepost: String? = null,
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
                "oppgaveId" to oppgaveId,
                "godkjent" to godkjent,
                "godkjenttidspunkt" to godkjenttidspunkt,
                "saksbehandlerident" to saksbehandlerident,
                "saksbehandlerepost" to saksbehandlerepost,
                "saksbehandleroid" to saksbehandlerOID,
                "saksbehandleroverstyringer" to saksbehandleroverstyringer,
                "saksbehandler" to mapOf(
                    "ident" to saksbehandlerident,
                    "epostadresse" to saksbehandlerepost,
                ),
            ).apply {
                årsak?.also { put("årsak", it) }
                begrunnelser?.also { put("begrunnelser", it) }
                kommentar?.also { put("kommentar", it) }
                beslutterident?.also {
                    put(
                        "beslutter",
                        mapOf(
                            "ident" to beslutterident,
                            "epostadresse" to beslutterepost,
                        )
                    )
                }
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
        organisasjonsnummer: String,
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
                    "inntekt" to mapOf(
                        "omregnetÅrsinntekt" to "123456.7",
                        "inntektskilde" to "Arbeidsgiver"
                    )
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

    fun lagAktivitetsloggNyAktivitet(
        fødselsnummer: String,
        id: UUID,
        vedtaksperiodeId: UUID,
        orgnummer: String,
        aktiviteter: List<Map<String, Any>> =
            listOf(
                lagVarsel(
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


    fun lagNyeVarsler(
        fødselsnummer: String,
        id: UUID,
        vedtaksperiodeId: UUID,
        orgnummer: String,
        aktiviteter: List<Map<String, Any>> =
            listOf(
                lagVarsel(
                    fødselsnummer = fødselsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    orgnummer = orgnummer,
                )
            ),
        ): String {
        return nyHendelse(
            id, "nye_varsler",
            mapOf(
                "fødselsnummer" to fødselsnummer,
                "aktiviteter" to aktiviteter
            )
        )
    }

    private fun lagVarsel(
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

    fun lagAvsluttetMedVedtak(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID,
        utbetalingId: UUID?,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        fastsattType: String,
        id: UUID,
        settInnAvviksvurderingFraSpleis: Boolean = true,
    ): String = nyHendelse(
        id, "avsluttet_med_vedtak", mutableMapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "behandlingId" to spleisBehandlingId,
            "fom" to fom,
            "tom" to tom,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "sykepengegrunnlag" to 600000.0,
            "vedtakFattetTidspunkt" to LocalDateTime.now(),
            "hendelser" to emptyList<String>()
        ).apply {
            compute("utbetalingId") { _, _ -> utbetalingId }
            if (utbetalingId != null) {
                val sykepengegrunnlagsfakta = when (fastsattType) {
                    "EtterSkjønn" -> fastsattEtterSkjønn(organisasjonsnummer, settInnAvviksvurderingFraSpleis)
                    "EtterHovedregel" -> fastsattEtterHovedregel(organisasjonsnummer, settInnAvviksvurderingFraSpleis)
                    "IInfotrygd" -> fastsattIInfotrygd()
                    else -> throw IllegalArgumentException("$fastsattType er ikke en gyldig fastsatt-type")
                }
                put("sykepengegrunnlagsfakta", sykepengegrunnlagsfakta)
            }
        }
    )

    fun lagAvsluttetUtenVedtak(
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        id: UUID,
    ): String = nyHendelse(
        id, "avsluttet_uten_vedtak", mutableMapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "fom" to fom,
            "tom" to tom,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "hendelser" to emptyList<String>(),
            "avsluttetTidspunkt" to LocalDateTime.now(),
            "behandlingId" to spleisBehandlingId,
        )
    )

    private fun fastsattEtterSkjønn(
        organisasjonsnummer: String,
        settInnAvviksvurderingFraSpleis: Boolean = true,
    ): Map<String, Any> {
        return mutableMapOf(
            "fastsatt" to "EtterSkjønn",
            "omregnetÅrsinntekt" to 500000.0,
            "skjønnsfastsatt" to 600000.0,
            "6G" to 6 * 118620.0,
            "arbeidsgivere" to listOf(
                mapOf(
                    "arbeidsgiver" to organisasjonsnummer,
                    "omregnetÅrsinntekt" to 500000.00,
                    "skjønnsfastsatt" to 600000.00
                )
            )
        ).apply {
            if (settInnAvviksvurderingFraSpleis) {
                this["innrapportertÅrsinntekt"] = 600000.0
                this["avviksprosent"] = 16.67
            }
        }

    }

    private fun fastsattEtterHovedregel(
        organisasjonsnummer: String,
        settInnAvviksvurderingFraSpleis: Boolean = true
    ): Map<String, Any> {
        return mutableMapOf(
            "fastsatt" to "EtterHovedregel",
            "omregnetÅrsinntekt" to 600000.0,
            "6G" to 6 * 118620.0,
            "arbeidsgivere" to listOf(
                mapOf(
                    "arbeidsgiver" to organisasjonsnummer,
                    "omregnetÅrsinntekt" to 600000.00,
                )
            )
        ).apply {
            if (settInnAvviksvurderingFraSpleis) {
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

    fun lagKommandokjedePåminnelse(commandContextId: UUID, meldingId: UUID, id: UUID) =
        nyHendelse(
            id, "kommandokjede_påminnelse", mapOf(
                "commandContextId" to commandContextId,
                "meldingId" to meldingId
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
