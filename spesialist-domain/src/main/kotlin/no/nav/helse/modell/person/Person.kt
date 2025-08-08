package no.nav.helse.modell.person

import no.nav.helse.modell.Meldingslogg
import no.nav.helse.modell.melding.Sykepengevedtak
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
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering.Companion.finnRiktigAvviksvurdering
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.flyttEventueltAvviksvarselTil
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
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
                                    erBegrensetTil6G = SykepengevedtakBuilder.TAG_6G_BEGRENSET in behandling.tags,
                                    `6G` = BigDecimal(fakta.seksG.toString()),
                                )
                            },
                    fom = behandling.fom(),
                    tom = behandling.tom(),
                    skjæringstidspunkt = behandling.skjæringstidspunkt(),
                    vedtakFattetTidspunkt =
                        avsluttetMedVedtak.vedtakFattetTidspunkt
                            .atZone(ZoneId.of("Europe/Oslo"))
                            .toInstant(),
                    utbetalingId = behandling.utbetalingId(),
                    vedtakBegrunnelse = behandling.vedtakBegrunnelse,
                )
            behandling.håndterVedtakFattet()
            meldingslogg.nyMelding(vedtak)
        } else {
            val vedtakBuilder = SykepengevedtakBuilder()
            vedtakBuilder.leggTilAvviksvurderinger(behandling)
            vedtakBuilder.leggTilSkjønnsmessigFastsettelse(behandling)

            vedtaksperiode.byggVedtak(vedtakBuilder)
            behandling.byggVedtak(vedtakBuilder)
            avsluttetMedVedtak.byggVedtak(vedtakBuilder)
            byggVedtak(vedtakBuilder)
            behandling.håndterVedtakFattet()
            avsluttetMedVedtak(vedtakBuilder.build())
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

    private fun SykepengevedtakBuilder.leggTilAvviksvurderinger(legacyBehandling: LegacyBehandling) {
        avviksvurderinger.finnRiktigAvviksvurdering(legacyBehandling.skjæringstidspunkt())?.let {
            sammenligningsgrunnlag(it.sammenligningsgrunnlag)
            avviksprosent(it.avviksprosent)
        }
    }

    private fun SykepengevedtakBuilder.leggTilSkjønnsmessigFastsettelse(legacyBehandling: LegacyBehandling) {
        skjønnsfastsatteSykepengegrunnlag.relevanteFor(legacyBehandling.skjæringstidspunkt()).lastOrNull()?.let {
            skjønnsfastsattSykepengegrunnlag(it)
        }
    }

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

    private fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.fødselsnummer(fødselsnummer)
        vedtakBuilder.aktørId(aktørId)
    }

    private fun avsluttetMedVedtak(vedtak: Sykepengevedtak) = meldingslogg.nyMelding(vedtak)

    companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)

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
