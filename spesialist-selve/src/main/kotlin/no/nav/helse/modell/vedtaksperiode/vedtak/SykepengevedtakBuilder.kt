package no.nav.helse.modell.vedtaksperiode.vedtak

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.sykefraværstilfelle.SkjønnsfastattSykepengegrunnlag
import kotlin.properties.Delegates

internal class SykepengevedtakBuilder {
    private lateinit var fødselsnummer: String
    private lateinit var aktørId: String
    private lateinit var vedtaksperiodeId: UUID
    private lateinit var organisasjonsnummer: String
    private lateinit var fom: LocalDate
    private lateinit var tom: LocalDate
    private lateinit var skjæringstidspunkt: LocalDate
    private lateinit var hendelser: List<UUID>
    private var sykepengegrunnlag by Delegates.notNull<Double>()
    private var grunnlagForSykepengegrunnlag by Delegates.notNull<Double>()
    private lateinit var grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>
    private lateinit var begrensning: String
    private var inntekt by Delegates.notNull<Double>()
    private lateinit var vedtakFattetTidspunkt: LocalDateTime
    private var utbetalingId: UUID? = null
    private var sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta? = null
    private var skjønnsfastsattSykepengegrunnlag: SkjønnsfastattSykepengegrunnlag? = null
    private var begrunnelseFraMal: String? = null
    private var begrunnelseFraFritekst: String? = null

    internal fun fødselsnummer(fødselsnummer: String) = apply { this.fødselsnummer = fødselsnummer }
    internal fun aktørId(aktørId: String) = apply { this.aktørId = aktørId }
    internal fun vedtaksperiodeId(vedtaksperiodeId: UUID) = apply { this.vedtaksperiodeId = vedtaksperiodeId }
    internal fun organisasjonsnummer(organisasjonsnummer: String) = apply { this.organisasjonsnummer = organisasjonsnummer }
    internal fun fom(fom: LocalDate) = apply { this.fom = fom }
    internal fun tom(tom: LocalDate) = apply { this.tom = tom }
    internal fun skjæringstidspunkt(skjæringstidspunkt: LocalDate) = apply { this.skjæringstidspunkt = skjæringstidspunkt }
    internal fun hendelser(hendelser: List<UUID>) = apply { this.hendelser = hendelser }
    internal fun sykepengegrunnlag(sykepengegrunnlag: Double) = apply { this.sykepengegrunnlag = sykepengegrunnlag }
    internal fun grunnlagForSykepengegrunnlag(grunnlagForSykepengegrunnlag: Double) = apply { this.grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag }
    internal fun grunnlagForSykepengegrunnlagPerArbeidsgiver(grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>) =
        apply { this.grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver }
    internal fun begrensning(begrensning: String) = apply { this.begrensning = begrensning }
    internal fun inntekt(inntekt: Double) = apply { this.inntekt = inntekt }
    internal fun vedtakFattetTidspunkt(vedtakFattetTidspunkt: LocalDateTime) = apply { this.vedtakFattetTidspunkt = vedtakFattetTidspunkt }
    internal fun utbetalingId(utbetalingId: UUID) = apply { this.utbetalingId = utbetalingId }
    internal fun sykepengegrunnlagsfakta(sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta) = apply { this.sykepengegrunnlagsfakta = sykepengegrunnlagsfakta }
    internal fun skjønnsfastsattSykepengegrunnlag(skjønnsfastsattSykepengegrunnlag: SkjønnsfastattSykepengegrunnlag) = apply {
        this.skjønnsfastsattSykepengegrunnlag = skjønnsfastsattSykepengegrunnlag
        skjønnsfastsattSykepengegrunnlag.byggVedtak(this)
    }
    internal fun begrunnelseFraMal(begrunnelseFraMal: String) = apply { this.begrunnelseFraMal = begrunnelseFraMal }
    internal fun begrunnelseFraFritekst(begrunnelseFraFritekst: String) = apply { this.begrunnelseFraFritekst = begrunnelseFraFritekst }

    internal fun build(): Sykepengevedtak {
        if (utbetalingId != null) return buildVedtak()
        require(sykepengegrunnlagsfakta == null) { "Forventer at sykepengegrunnlagsfakta er null for Auu-vedtak" }
        return buildAuuVedtak()
    }

    private fun buildAuuVedtak(): Sykepengevedtak.AuuVedtak {
        return Sykepengevedtak.AuuVedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = hendelser,
            sykepengegrunnlag = sykepengegrunnlag,
            grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning = begrensning,
            inntekt = inntekt,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt
        )
    }

    private fun buildVedtak(): Sykepengevedtak.Vedtak {
        val sykepengegrunnlagsfakta = requireNotNull(sykepengegrunnlagsfakta) { "Forventer å finne sykepengegrunnlagsfakta ved bygging av vedtak" }
        val utbetalingId = requireNotNull(utbetalingId) { "Forventer å finne utbetalingId ved bygging av vedtak" }
        if (sykepengegrunnlagsfakta is Sykepengegrunnlagsfakta.Spleis.EtterSkjønn)
            return buildVedtakEtterSkjønn(sykepengegrunnlagsfakta, utbetalingId)
        return buildVedtak(sykepengegrunnlagsfakta, utbetalingId)
    }

    private fun buildVedtakEtterSkjønn(sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta, utbetalingId: UUID): Sykepengevedtak.Vedtak {
        val begrunnelseFraMal = requireNotNull(begrunnelseFraMal) { "Forventer å finne begrunnelse fra mal ved bygging av vedtak når sykepengegrunnlaget er fastsatt etter skjønn" }
        val begrunnelseFraFritekst = requireNotNull(begrunnelseFraFritekst) { "Forventer å finne begrunnelse fra fritekst ved bygging av vedtak når sykepengegrunnlaget er fastsatt etter skjønn" }

        return Sykepengevedtak.Vedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = hendelser,
            sykepengegrunnlag = sykepengegrunnlag,
            grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning = begrensning,
            inntekt = inntekt,
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
            begrunnelseFraFritekst = begrunnelseFraFritekst,
            begrunnelseFraMal = begrunnelseFraMal
        )
    }

    private fun buildVedtak(sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta, utbetalingId: UUID): Sykepengevedtak.Vedtak {
        return Sykepengevedtak.Vedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = hendelser,
            sykepengegrunnlag = sykepengegrunnlag,
            grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning = begrensning,
            inntekt = inntekt,
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
            begrunnelseFraFritekst = null,
            begrunnelseFraMal = null
        )
    }
}