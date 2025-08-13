package no.nav.helse.modell.person

import no.nav.helse.modell.Meldingslogg
import no.nav.helse.modell.melding.Sykepengevedtak.Vedtak
import no.nav.helse.modell.melding.Sykepengevedtak.VedtakMedOpphavIInfotrygd
import no.nav.helse.modell.melding.Sykepengevedtak.VedtakMedSkjønnsvurdering
import no.nav.helse.modell.melding.SykepengevedtakSelvstendigNæringsdrivendeDto
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode.Companion.finnBehandling
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode.Companion.relevanteFor
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtak.AvsluttetMedVedtak
import no.nav.helse.modell.vedtak.AvsluttetUtenVedtak
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag.Companion.relevanteFor
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering.Companion.finnRiktigAvviksvurdering
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.flyttEventueltAvviksvarselTil
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class Person private constructor(
    private val aktørId: String,
    val fødselsnummer: String,
    vedtaksperioder: List<Vedtaksperiode>,
    private val skjønnsfastsatteSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlag>,
    private val avviksvurderinger: List<Avviksvurdering>,
) {
    private val vedtaksperioder = vedtaksperioder.toMutableList()
    private val meldingslogg = Meldingslogg()

    fun toDto() =
        PersonDto(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            vedtaksperioder = vedtaksperioder.map { it.toDto() },
            avviksvurderinger = avviksvurderinger,
            skjønnsfastsatteSykepengegrunnlag = skjønnsfastsatteSykepengegrunnlag.map { it.toDto() },
        )

    fun utgåendeMeldinger() = meldingslogg.hendelser()

    fun forkastedeVedtaksperiodeIder() =
        vedtaksperioder
            .filter {
                it.erForkastet()
            }.map { it.vedtaksperiodeId() }

    fun flyttEventuelleAvviksvarsler(
        vedtaksperiodeId: UUID,
        skjæringstidspunkt: LocalDate,
    ) {
        vedtaksperioder.relevanteFor(skjæringstidspunkt).flyttEventueltAvviksvarselTil(vedtaksperiodeId)
    }

    fun behandleTilbakedateringBehandlet(perioder: List<Periode>) {
        vedtaksperioder.forEach { it.behandleTilbakedateringGodkjent(perioder) }
    }

    fun mottaSpleisVedtaksperioder(perioder: List<SpleisVedtaksperiode>) {
        vedtaksperioder.forEach { it.nyttGodkjenningsbehov(perioder) }
    }

    fun oppdaterPeriodeTilGodkjenning(
        vedtaksperiodeId: UUID,
        tags: List<String>,
        spleisBehandlingId: UUID,
        utbetalingId: UUID,
    ) {
        vedtaksperiodeOrNull(vedtaksperiodeId)?.mottaBehandlingsinformasjon(tags, spleisBehandlingId, utbetalingId)
    }

    fun avsluttetMedVedtak(avsluttetMedVedtak: AvsluttetMedVedtak) {
        val vedtaksperiode = vedtaksperiodeForBehandling(avsluttetMedVedtak.spleisBehandlingId)
        val behandling = vedtaksperiode.finnBehandling(avsluttetMedVedtak.spleisBehandlingId)
        val tag6gBegrenset = "6GBegrenset"

        if (avsluttetMedVedtak.yrkesaktivitetstype.lowercase() == "selvstendig") {
            val vedtak =
                SykepengevedtakSelvstendigNæringsdrivendeDto(
                    fødselsnummer = fødselsnummer,
                    vedtaksperiodeId = behandling.vedtaksperiodeId(),
                    sykepengegrunnlag = BigDecimal(avsluttetMedVedtak.sykepengegrunnlag.toString()),
                    sykepengegrunnlagsfakta =
                        (avsluttetMedVedtak.sykepengegrunnlagsfakta as Sykepengegrunnlagsfakta.Spleis)
                            .let { fakta ->
                                SykepengevedtakSelvstendigNæringsdrivendeDto.Sykepengegrunnlagsfakta(
                                    beregningsgrunnlag =
                                        BigDecimal(
                                            fakta.arbeidsgivere
                                                .single { it.organisasjonsnummer.lowercase() == "selvstendig" }
                                                .omregnetÅrsinntekt
                                                .toString(),
                                        ),
                                    pensjonsgivendeInntekter = emptyList(),
                                    erBegrensetTil6G = tag6gBegrenset in behandling.tags,
                                    `6G` = BigDecimal(fakta.seksG.toString()),
                                )
                            },
                    fom = behandling.fom(),
                    tom = behandling.tom(),
                    skjæringstidspunkt = behandling.skjæringstidspunkt(),
                    hendelser = avsluttetMedVedtak.hendelser,
                    vedtakFattetTidspunkt = avsluttetMedVedtak.vedtakFattetTidspunkt,
                    utbetalingId = behandling.utbetalingId(),
                    vedtakBegrunnelse = behandling.vedtakBegrunnelse,
                )
            behandling.håndterVedtakFattet()
            meldingslogg.nyMelding(vedtak)
        } else {
            if (behandling.tags.isEmpty()) {
                sikkerlogg.error(
                    "Ingen tags funnet for spleisBehandlingId: ${behandling.spleisBehandlingId} på vedtaksperiodeId: ${behandling.vedtaksperiodeId}",
                )
            }
            val melding =
                when (val sykepengegrunnlagsfakta = avsluttetMedVedtak.sykepengegrunnlagsfakta) {
                    is Sykepengegrunnlagsfakta.Infotrygd -> {
                        VedtakMedOpphavIInfotrygd(
                            fødselsnummer = fødselsnummer,
                            aktørId = aktørId,
                            organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                            vedtaksperiodeId = behandling.vedtaksperiodeId,
                            spleisBehandlingId = behandling.behandlingId(),
                            utbetalingId = behandling.utbetalingId(),
                            fom = behandling.fom(),
                            tom = behandling.tom(),
                            skjæringstidspunkt = behandling.skjæringstidspunkt,
                            hendelser = avsluttetMedVedtak.hendelser,
                            sykepengegrunnlag = avsluttetMedVedtak.sykepengegrunnlag,
                            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
                            vedtakFattetTidspunkt = avsluttetMedVedtak.vedtakFattetTidspunkt,
                            tags = behandling.tags.toSet(),
                            vedtakBegrunnelse = behandling.vedtakBegrunnelse,
                        )
                    }

                    is Sykepengegrunnlagsfakta.Spleis -> {
                        val avviksvurdering =
                            avviksvurderinger.finnRiktigAvviksvurdering(behandling.skjæringstidspunkt())
                                ?: error("Fant ikke avviksvurdering for vedtak")

                        val (tagsForSykepengegrunnlagsfakta, tagsForPeriode) = behandling.tags.partition { it == tag6gBegrenset }
                        sykepengegrunnlagsfakta.leggTilTags(tagsForSykepengegrunnlagsfakta.toSet())
                        when (sykepengegrunnlagsfakta) {
                            is Sykepengegrunnlagsfakta.Spleis.EtterHovedregel -> {
                                Vedtak(
                                    fødselsnummer = fødselsnummer,
                                    aktørId = aktørId,
                                    organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                                    vedtaksperiodeId = behandling.vedtaksperiodeId,
                                    spleisBehandlingId = behandling.behandlingId(),
                                    utbetalingId = behandling.utbetalingId(),
                                    fom = behandling.fom(),
                                    tom = behandling.tom(),
                                    skjæringstidspunkt = behandling.skjæringstidspunkt,
                                    hendelser = avsluttetMedVedtak.hendelser,
                                    sykepengegrunnlag = avsluttetMedVedtak.sykepengegrunnlag,
                                    sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
                                    vedtakFattetTidspunkt = avsluttetMedVedtak.vedtakFattetTidspunkt,
                                    tags = tagsForPeriode.toSet(),
                                    vedtakBegrunnelse = behandling.vedtakBegrunnelse,
                                    avviksprosent = avviksvurdering.avviksprosent,
                                    sammenligningsgrunnlag = avviksvurdering.sammenligningsgrunnlag,
                                )
                            }

                            is Sykepengegrunnlagsfakta.Spleis.EtterSkjønn -> {
                                VedtakMedSkjønnsvurdering(
                                    fødselsnummer = fødselsnummer,
                                    aktørId = aktørId,
                                    organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                                    vedtaksperiodeId = behandling.vedtaksperiodeId,
                                    spleisBehandlingId = behandling.behandlingId(),
                                    utbetalingId = behandling.utbetalingId(),
                                    fom = behandling.fom(),
                                    tom = behandling.tom(),
                                    skjæringstidspunkt = behandling.skjæringstidspunkt,
                                    hendelser = avsluttetMedVedtak.hendelser,
                                    sykepengegrunnlag = avsluttetMedVedtak.sykepengegrunnlag,
                                    sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
                                    skjønnsfastsettingopplysninger =
                                        skjønnsfastsatteSykepengegrunnlag
                                            .relevanteFor(
                                                skjæringstidspunkt = behandling.skjæringstidspunkt(),
                                            ).lastOrNull()
                                            ?.let {
                                                VedtakMedSkjønnsvurdering.Skjønnsfastsettingopplysninger(
                                                    begrunnelseFraMal = it.begrunnelseFraMal,
                                                    begrunnelseFraFritekst = it.begrunnelseFraFritekst,
                                                    begrunnelseFraKonklusjon = it.begrunnelseFraKonklusjon,
                                                    skjønnsfastsettingtype = it.type,
                                                    skjønnsfastsettingsårsak = it.årsak,
                                                )
                                            }
                                            ?: error("Forventer å finne opplysninger fra saksbehandler ved bygging av vedtak når sykepengegrunnlaget er fastsatt etter skjønn"),
                                    vedtakFattetTidspunkt = avsluttetMedVedtak.vedtakFattetTidspunkt,
                                    tags = tagsForPeriode.toSet(),
                                    vedtakBegrunnelse = behandling.vedtakBegrunnelse,
                                    avviksprosent = avviksvurdering.avviksprosent,
                                    sammenligningsgrunnlag = avviksvurdering.sammenligningsgrunnlag,
                                )
                            }
                        }
                    }
                }
            meldingslogg.nyMelding(melding)
            behandling.håndterVedtakFattet()
        }
    }

    fun avsluttetUtenVedtak(avsluttetUtenVedtak: AvsluttetUtenVedtak) {
        val vedtaksperiode = vedtaksperiodeForBehandling(avsluttetUtenVedtak.spleisBehandlingId)
        if (vedtaksperiode.erForkastet()) return

        vedtaksperiode
            .finnBehandling(avsluttetUtenVedtak.spleisBehandlingId)
            .avsluttetUtenVedtak()
    }

    fun behandlinger() = vedtaksperioder.flatMap { it.behandlinger() }

    fun vedtaksperiodeForkastet(vedtaksperiodeId: UUID) {
        vedtaksperioder
            .find { it.vedtaksperiodeId() == vedtaksperiodeId }
            ?.vedtaksperiodeForkastet()
    }

    fun nySpleisBehandling(spleisBehandling: SpleisBehandling) {
        vedtaksperioder
            .find { spleisBehandling.erRelevantFor(it.vedtaksperiodeId()) }
            ?.nySpleisBehandling(spleisBehandling)
            ?: vedtaksperioder.add(Vedtaksperiode.nyVedtaksperiode(spleisBehandling))
    }

    fun vedtaksperiodeOrNull(vedtaksperiodeId: UUID): Vedtaksperiode? {
        return vedtaksperioder.find { it.vedtaksperiodeId() == vedtaksperiodeId }
            ?: logg.warn("Vedtaksperiode med id={} finnes ikke", vedtaksperiodeId).let { return null }
    }

    fun vedtaksperiode(vedtaksperiodeId: UUID): Vedtaksperiode {
        val vedtaksperiode = vedtaksperiodeOrNull(vedtaksperiodeId)
        return checkNotNull(vedtaksperiode)
    }

    private fun vedtaksperiodeForBehandling(spleisBehandlingId: UUID): Vedtaksperiode =
        vedtaksperioder.finnBehandling(spleisBehandlingId)
            ?: throw IllegalStateException("Behandling med spleisBehandlingId=$spleisBehandlingId finnes ikke")

    fun sykefraværstilfelle(vedtaksperiodeId: UUID): Sykefraværstilfelle {
        val skjæringstidspunkt =
            vedtaksperiodeOrNull(vedtaksperiodeId)?.gjeldendeSkjæringstidspunkt
                ?: throw IllegalStateException("Forventer å finne vedtaksperiode med id=$vedtaksperiodeId")
        val gjeldendeBehandlinger = vedtaksperioder.relevanteFor(skjæringstidspunkt)
        return Sykefraværstilfelle(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            gjeldendeBehandlinger = gjeldendeBehandlinger,
        )
    }

    fun nyeVarsler(varsler: List<Varsel>) {
        vedtaksperioder.forEach { it.nyeVarsler(varsler) }
    }

    fun utbetalingForkastet(utbetalingId: UUID) {
        vedtaksperioder.forEach {
            it.utbetalingForkastet(utbetalingId)
        }
    }

    fun nyUtbetalingForVedtaksperiode(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        vedtaksperiodeOrNull(vedtaksperiodeId)
            ?.nyUtbetaling(utbetalingId)
    }

    companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        fun gjenopprett(
            aktørId: String,
            fødselsnummer: String,
            vedtaksperioder: List<VedtaksperiodeDto>,
            skjønnsfastsattSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlagDto>,
            avviksvurderinger: List<Avviksvurdering>,
        ): Person =
            Person(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                vedtaksperioder =
                    vedtaksperioder.map {
                        Vedtaksperiode.gjenopprett(
                            organisasjonsnummer = it.organisasjonsnummer,
                            vedtaksperiodeId = it.vedtaksperiodeId,
                            forkastet = it.forkastet,
                            behandlinger = it.behandlinger,
                        )
                    },
                avviksvurderinger = avviksvurderinger,
                skjønnsfastsatteSykepengegrunnlag =
                    skjønnsfastsattSykepengegrunnlag
                        .sortedBy { it.opprettet }
                        .map {
                            SkjønnsfastsattSykepengegrunnlag.gjenopprett(
                                type = it.type,
                                årsak = it.årsak,
                                skjæringstidspunkt = it.skjæringstidspunkt,
                                begrunnelseFraMal = it.begrunnelseFraMal,
                                begrunnelseFraFritekst = it.begrunnelseFraFritekst,
                                begrunnelseFraKonklusjon = it.begrunnelseFraKonklusjon,
                                opprettet = it.opprettet,
                            )
                        },
            )
    }
}
