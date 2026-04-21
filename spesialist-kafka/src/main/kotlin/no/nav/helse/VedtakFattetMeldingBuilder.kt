package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.asBigDecimal
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.melding.SaksbehandlerIdentOgNavn
import no.nav.helse.modell.melding.VedtakFattetMelding
import no.nav.helse.modell.melding.VedtakFattetMelding.SelvstendigNæringsdrivendeSykepengegrunnlagsfakta.PensjonsgivendeInntekt
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.Skjønnsfastsettingstype
import no.nav.helse.modell.vedtak.Skjønnsfastsettingsårsak
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtaksperiode.Arbeidssituasjon
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.spesialist.application.ForsikringHenter
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.ResultatAvForsikring
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtak
import java.math.BigDecimal
import java.util.UUID

class VedtakFattetMeldingBuilder(
    private val identitetsnummer: Identitetsnummer,
    private val sessionContext: SessionContext,
    private val behandlingId: SpleisBehandlingId,
    private val packet: JsonMessage,
    private val forsikringHenter: ForsikringHenter,
    private val environmentToggles: EnvironmentToggles,
) {
    companion object {
        private const val FASTSATT_ETTER_HOVEDREGEL = "EtterHovedregel"
        private const val FASTSATT_ETTER_SKJØNN = "EtterSkjønn"
        private const val FASTSATT_I_INFOTRYGD = "IInfotrygd"

        internal const val YRKESAKTIVITETSTYPE_SELVSTENDIG_NÆRINGSDRIVENDE = "SELVSTENDIG"
        private const val ORGANISASJONSNUMMER_SELVSTENDIG_NÆRINGSDRIVENDE = "SELVSTENDIG"
        private const val TAG_6G_BEGRENSET = "6GBegrenset"
    }

    private val person = sessionContext.personRepository.finn(identitetsnummer) ?: error("Fant ikke person")
    private val behandling = sessionContext.behandlingRepository.finn(behandlingId) ?: error("Fant ikke behandling")
    private val vedtak = sessionContext.vedtakRepository.finn(behandlingId) ?: error("Fant ikke vedtaksinformasjon")
    private val godkjenningsbehov = sessionContext.meldingDao.finnSisteGodkjenningsbehov(behandlingId.value) ?: error("Fant ikke siste godkjenningsbehov")
    private val fastsatt = packet["sykepengegrunnlagsfakta"]["fastsatt"].asText()

    private fun byggFellesdel(
        organisasjonsnummer: String,
        yrkesaktivitetstype: String,
        sykepengegrunnlagsfakta: VedtakFattetMelding.Sykepengegrunnlagsfakta,
        dekning: VedtakFattetMelding.Dekning?,
        begrunnelser: List<VedtakFattetMelding.Begrunnelse>,
    ): VedtakFattetMelding =
        when (vedtak) {
            is Vedtak.Automatisk -> {
                byggFellesdel(
                    organisasjonsnummer = organisasjonsnummer,
                    yrkesaktivitetstype = yrkesaktivitetstype,
                    sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
                    dekning = dekning,
                    saksbehandlerIdentOgNavn = null,
                    beslutterIdentOgNavn = null,
                    automatiskFattet = true,
                    begrunnelser = begrunnelser,
                )
            }

            is Vedtak.ManueltMedTotrinnskontroll -> {
                val saksbehandler =
                    sessionContext.saksbehandlerRepository.finn(vedtak.saksbehandlerIdent)
                        ?: error("Finner ikke saksbehandler")
                val beslutter =
                    sessionContext.saksbehandlerRepository.finn(vedtak.beslutterIdent)
                        ?: error("Finner ikke beslutter")
                byggFellesdel(
                    organisasjonsnummer = organisasjonsnummer,
                    yrkesaktivitetstype = yrkesaktivitetstype,
                    sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
                    dekning = dekning,
                    saksbehandlerIdentOgNavn = SaksbehandlerIdentOgNavn(saksbehandler.ident.value, saksbehandler.navn),
                    beslutterIdentOgNavn = SaksbehandlerIdentOgNavn(beslutter.ident.value, beslutter.navn),
                    automatiskFattet = false,
                    begrunnelser = begrunnelser,
                )
            }

            is Vedtak.ManueltUtenTotrinnskontroll -> {
                val saksbehandler =
                    sessionContext.saksbehandlerRepository.finn(vedtak.saksbehandlerIdent)
                        ?: error("Finner ikke saksbehandler")
                byggFellesdel(
                    organisasjonsnummer = organisasjonsnummer,
                    yrkesaktivitetstype = yrkesaktivitetstype,
                    sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
                    dekning = dekning,
                    saksbehandlerIdentOgNavn = SaksbehandlerIdentOgNavn(saksbehandler.ident.value, saksbehandler.navn),
                    beslutterIdentOgNavn = null,
                    automatiskFattet = false,
                    begrunnelser = begrunnelser,
                )
            }
        }

    private fun byggFellesdel(
        organisasjonsnummer: String,
        yrkesaktivitetstype: String,
        sykepengegrunnlagsfakta: VedtakFattetMelding.Sykepengegrunnlagsfakta,
        dekning: VedtakFattetMelding.Dekning?,
        saksbehandlerIdentOgNavn: SaksbehandlerIdentOgNavn?,
        beslutterIdentOgNavn: SaksbehandlerIdentOgNavn?,
        automatiskFattet: Boolean,
        begrunnelser: List<VedtakFattetMelding.Begrunnelse>,
    ): VedtakFattetMelding {
        val utbetalingId = checkNotNull(behandling.utbetalingId)
        val individuellBegrunnelse =
            sessionContext.individuellBegrunnelseRepository.finn(behandlingId)?.let {
                VedtakFattetMelding.Begrunnelse(
                    type =
                        when (it.utfall) {
                            Utfall.AVSLAG -> VedtakFattetMelding.BegrunnelseType.Avslag
                            Utfall.DELVIS_INNVILGELSE -> VedtakFattetMelding.BegrunnelseType.DelvisInnvilgelse
                            Utfall.INNVILGELSE -> VedtakFattetMelding.BegrunnelseType.Innvilgelse
                        },
                    begrunnelse = it.tekst,
                )
            }
        return VedtakFattetMelding(
            fødselsnummer = identitetsnummer.value,
            aktørId = person.aktørId,
            organisasjonsnummer = organisasjonsnummer,
            yrkesaktivitetstype = yrkesaktivitetstype,
            vedtaksperiodeId = behandling.vedtaksperiodeId.value,
            behandlingId = behandlingId.value,
            utbetalingId = utbetalingId.value,
            fom = behandling.fom,
            tom = behandling.tom,
            skjæringstidspunkt = behandling.skjæringstidspunkt,
            hendelser = packet["hendelser"].map<JsonNode, UUID> { it.asUUID() },
            sykepengegrunnlag = packet["sykepengegrunnlagsfakta"]["sykepengegrunnlag"].asBigDecimal(),
            vedtakFattetTidspunkt = packet["vedtakFattetTidspunkt"].asLocalDateTime(),
            tags = behandling.tags.filterNot { it == TAG_6G_BEGRENSET }.toSet(),
            begrunnelser = begrunnelser + listOfNotNull(individuellBegrunnelse),
            saksbehandler = saksbehandlerIdentOgNavn,
            beslutter = beslutterIdentOgNavn,
            automatiskFattet = automatiskFattet,
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
            dekning = dekning,
        )
    }

    private fun dekning(): VedtakFattetMelding.Dekning? {
        fun defaultDekning(): VedtakFattetMelding.Dekning =
            VedtakFattetMelding.Dekning(
                dekningsgrad = if (godkjenningsbehov.arbeidssituasjon == Arbeidssituasjon.JORDBRUKER) 100 else 80,
                gjelderFraDag = 17,
            )

        if (!environmentToggles.kanSeForsikring) return defaultDekning()

        return when (val resultatAvForsikring = forsikringHenter.hentForsikringsinformasjon(behandlingId)) {
            is ResultatAvForsikring.MottattForsikring -> {
                VedtakFattetMelding.Dekning(
                    resultatAvForsikring.forsikring.dekningsgrad,
                    resultatAvForsikring.forsikring.gjelderFraDag,
                )
            }

            ResultatAvForsikring.IngenForsikring -> {
                defaultDekning()
            }
        }
    }

    private fun byggSelvstendigNæringsdrivendeSykepengegrunnlagsfakta(
        sykepengegrunnlagsfakta: Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.SelvstendigNæringsdrivende,
    ): VedtakFattetMelding.SelvstendigNæringsdrivendeSykepengegrunnlagsfakta {
        check(fastsatt == FASTSATT_ETTER_HOVEDREGEL) {
            "Ustøttet verdi sykepengegrunnlagsfakta.fastsatt for selvstendig næringsdrivende: \"$fastsatt\"." +
                " Kun \"${FASTSATT_ETTER_HOVEDREGEL}\" støttes."
        }
        return VedtakFattetMelding.SelvstendigNæringsdrivendeSykepengegrunnlagsfakta(
            beregningsgrunnlag =
                BigDecimal(
                    packet["sykepengegrunnlagsfakta"]["selvstendig"]["beregningsgrunnlag"].asText(),
                ),
            tags = behandling.tags.filter { it == TAG_6G_BEGRENSET }.toSet(),
            seksG = BigDecimal(packet["sykepengegrunnlagsfakta"]["6G"].asText()),
            pensjonsgivendeInntekter =
                sykepengegrunnlagsfakta.selvstendig.pensjonsgivendeInntekter.map { inntekt ->
                    PensjonsgivendeInntekt(
                        årstall = inntekt.årstall,
                        beløp = inntekt.beløp,
                    )
                },
        )
    }

    fun byggVedtakFattetMeldingForSelvstendig(): VedtakFattetMelding {
        val dekning = dekning()
        val sykepengegrunnlagsfakta =
            byggSelvstendigNæringsdrivendeSykepengegrunnlagsfakta(
                sykepengegrunnlagsfakta = godkjenningsbehov.sykepengegrunnlagsfakta as Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.SelvstendigNæringsdrivende,
            )
        return byggFellesdel(
            organisasjonsnummer = ORGANISASJONSNUMMER_SELVSTENDIG_NÆRINGSDRIVENDE,
            yrkesaktivitetstype = YRKESAKTIVITETSTYPE_SELVSTENDIG_NÆRINGSDRIVENDE,
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
            dekning = dekning,
            begrunnelser = emptyList(),
        )
    }

    private fun byggFastsattEtterHovedregelSykepengegrunnlagsfakta(
        packet: JsonMessage,
        avviksvurdering: Avviksvurdering,
    ) = VedtakFattetMelding.FastsattEtterHovedregelSykepengegrunnlagsfakta(
        omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta"]["omregnetÅrsinntektTotalt"].asBigDecimal(),
        seksG = packet["sykepengegrunnlagsfakta"]["6G"].asBigDecimal(),
        tags = behandling.tags.filter { it == TAG_6G_BEGRENSET }.toSet(),
        avviksprosent = avviksvurdering.avviksprosent.toBigDecimal(),
        innrapportertÅrsinntekt = avviksvurdering.sammenligningsgrunnlag.totalbeløp.toBigDecimal(),
        arbeidsgivere =
            packet["sykepengegrunnlagsfakta"]["arbeidsgivere"].map { arbeidsgiver ->
                val arbeidsgiverreferanse = arbeidsgiver["arbeidsgiver"].asText()
                VedtakFattetMelding.FastsattEtterHovedregelSykepengegrunnlagsfakta.Arbeidsgiver(
                    organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText(),
                    omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asBigDecimal(),
                    innrapportertÅrsinntekt =
                        avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter
                            .filter { it.arbeidsgiverreferanse == arbeidsgiverreferanse }
                            .flatMap(InnrapportertInntekt::inntekter)
                            .sumOf(Inntekt::beløp)
                            .toBigDecimal(),
                )
            },
    )

    private fun byggFastsattEtterSkjønnSykepengegrunnlagsfakta(
        packet: JsonMessage,
        skjønnsfastsattSykepengegrunnlag: SkjønnsfastsattSykepengegrunnlag,
        avviksvurdering: Avviksvurdering,
        tags: Set<String>,
    ) = VedtakFattetMelding.FastsattEtterSkjønnSykepengegrunnlagsfakta(
        omregnetÅrsinntektTotalt = packet["sykepengegrunnlagsfakta"]["omregnetÅrsinntektTotalt"].asBigDecimal(),
        seksG = packet["sykepengegrunnlagsfakta"]["6G"].asBigDecimal(),
        avviksprosent = avviksvurdering.avviksprosent.toBigDecimal(),
        innrapportertÅrsinntekt = avviksvurdering.sammenligningsgrunnlag.totalbeløp.toBigDecimal(),
        tags = tags.filter { it == TAG_6G_BEGRENSET }.toSet(),
        skjønnsfastsatt = packet["sykepengegrunnlagsfakta"]["skjønnsfastsatt"].asBigDecimal(),
        skjønnsfastsettingtype =
            when (skjønnsfastsattSykepengegrunnlag.type) {
                Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT -> VedtakFattetMelding.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
                Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT -> VedtakFattetMelding.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
                Skjønnsfastsettingstype.ANNET -> VedtakFattetMelding.Skjønnsfastsettingstype.ANNET
            },
        skjønnsfastsettingsårsak =
            when (skjønnsfastsattSykepengegrunnlag.årsak) {
                Skjønnsfastsettingsårsak.ANDRE_AVSNITT -> VedtakFattetMelding.Skjønnsfastsettingsårsak.ANDRE_AVSNITT
                Skjønnsfastsettingsårsak.TREDJE_AVSNITT -> VedtakFattetMelding.Skjønnsfastsettingsårsak.TREDJE_AVSNITT
            },
        arbeidsgivere =
            packet["sykepengegrunnlagsfakta"]["arbeidsgivere"].map { arbeidsgiver ->
                val organisasjonsnummer = arbeidsgiver["arbeidsgiver"].asText()
                VedtakFattetMelding.FastsattEtterSkjønnSykepengegrunnlagsfakta.Arbeidsgiver(
                    organisasjonsnummer = organisasjonsnummer,
                    omregnetÅrsinntekt = arbeidsgiver["omregnetÅrsinntekt"].asBigDecimal(),
                    innrapportertÅrsinntekt =
                        avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter
                            .filter { it.arbeidsgiverreferanse == organisasjonsnummer }
                            .flatMap(InnrapportertInntekt::inntekter)
                            .sumOf(Inntekt::beløp)
                            .toBigDecimal(),
                    skjønnsfastsatt = arbeidsgiver["skjønnsfastsatt"].asBigDecimal(),
                )
            },
    )

    private fun byggFastsattIInfotrygdSykepengegrunnlagsfakta(packet: JsonMessage) =
        VedtakFattetMelding.FastsattIInfotrygdSykepengegrunnlagsfakta(
            omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta"]["omregnetÅrsinntektTotalt"].asBigDecimal(),
        )

    private fun finnAvviksvurdering(): Avviksvurdering =
        sessionContext.avviksvurderingRepository
            .finnAvviksvurderinger(identitetsnummer.value)
            .filter { it.skjæringstidspunkt == behandling.skjæringstidspunkt }
            .maxBy { it.opprettet }

    fun byggVedtakFattetMeldingForArbeidstaker(
        skjønnsfastsatteSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlag>,
    ): VedtakFattetMelding {
        val vedtaksperiode = sessionContext.vedtaksperiodeRepository.finn(behandling.vedtaksperiodeId) ?: error("Fant ikke vedtaksperiode")
        val sykepengegrunnlagsfakta =
            when (fastsatt) {
                FASTSATT_I_INFOTRYGD -> {
                    byggFastsattIInfotrygdSykepengegrunnlagsfakta(packet = packet)
                }

                FASTSATT_ETTER_HOVEDREGEL -> {
                    byggFastsattEtterHovedregelSykepengegrunnlagsfakta(
                        packet = packet,
                        avviksvurdering = finnAvviksvurdering(),
                    )
                }

                FASTSATT_ETTER_SKJØNN -> {
                    val skjønnsfastsattSykepengegrunnlag =
                        skjønnsfastsatteSykepengegrunnlag.last { it.skjæringstidspunkt == behandling.skjæringstidspunkt }
                    byggFastsattEtterSkjønnSykepengegrunnlagsfakta(
                        packet = packet,
                        tags = behandling.tags,
                        skjønnsfastsattSykepengegrunnlag = skjønnsfastsattSykepengegrunnlag,
                        avviksvurdering = finnAvviksvurdering(),
                    )
                }

                else -> {
                    error("Ukjent verdi for sykepengegrunnlagsfakta.fastsatt: \"$fastsatt\"")
                }
            }

        val begrunnelser =
            if (fastsatt == FASTSATT_ETTER_SKJØNN) {
                val skjønnsfastsattSykepengegrunnlag =
                    skjønnsfastsatteSykepengegrunnlag.last { it.skjæringstidspunkt == behandling.skjæringstidspunkt }

                listOf(
                    VedtakFattetMelding.Begrunnelse(
                        type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagMal,
                        begrunnelse = skjønnsfastsattSykepengegrunnlag.begrunnelseFraMal,
                    ),
                    VedtakFattetMelding.Begrunnelse(
                        type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagFritekst,
                        begrunnelse = skjønnsfastsattSykepengegrunnlag.begrunnelseFraFritekst,
                    ),
                    VedtakFattetMelding.Begrunnelse(
                        type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagKonklusjon,
                        begrunnelse = skjønnsfastsattSykepengegrunnlag.begrunnelseFraKonklusjon,
                    ),
                )
            } else {
                emptyList()
            }

        return byggFellesdel(
            organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
            yrkesaktivitetstype =
                when (behandling.yrkesaktivitetstype) {
                    Yrkesaktivitetstype.ARBEIDSTAKER -> "ARBEIDSTAKER"
                    Yrkesaktivitetstype.FRILANS -> "FRILANS"
                    Yrkesaktivitetstype.ARBEIDSLEDIG -> "ARBEIDSLEDIG"
                    Yrkesaktivitetstype.SELVSTENDIG -> "SELVSTENDIG"
                },
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
            dekning = null,
            begrunnelser = begrunnelser,
        )
    }
}
