package no.nav.helse.modell.vedtak

import no.nav.helse.modell.melding.Sykepengevedtak
import no.nav.helse.modell.melding.Sykepengevedtak.VedtakMedSkjønnsvurdering
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.properties.Delegates

class SykepengevedtakBuilder {
    private lateinit var fødselsnummer: String
    private lateinit var aktørId: String
    private lateinit var vedtaksperiodeId: UUID
    private lateinit var spleisBehandlingId: UUID
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
    private lateinit var utbetalingId: UUID
    private lateinit var sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta
    private var skjønnsfastsattSykepengegrunnlag: SkjønnsfastsattSykepengegrunnlag? = null
    private var skjønnsfastsettingopplysninger: VedtakMedSkjønnsvurdering.Skjønnsfastsettingopplysninger? = null
    private var vedtakBegrunnelse: VedtakBegrunnelse? = null
    private var avviksprosent by Delegates.notNull<Double>()
    private lateinit var sammenligningsgrunnlag: Sammenligningsgrunnlag
    private val tags: MutableSet<String> = mutableSetOf()
    private val tagsForSykepengegrunnlagsfakta: MutableSet<String> = mutableSetOf()

    fun fødselsnummer(fødselsnummer: String) = apply { this.fødselsnummer = fødselsnummer }

    fun aktørId(aktørId: String) = apply { this.aktørId = aktørId }

    fun vedtaksperiodeId(vedtaksperiodeId: UUID) = apply { this.vedtaksperiodeId = vedtaksperiodeId }

    fun spleisBehandlingId(spleisBehandlingId: UUID) = apply { this.spleisBehandlingId = spleisBehandlingId }

    fun organisasjonsnummer(organisasjonsnummer: String) = apply { this.organisasjonsnummer = organisasjonsnummer }

    fun fom(fom: LocalDate) = apply { this.fom = fom }

    fun tom(tom: LocalDate) = apply { this.tom = tom }

    fun skjæringstidspunkt(skjæringstidspunkt: LocalDate) = apply { this.skjæringstidspunkt = skjæringstidspunkt }

    fun hendelser(hendelser: List<UUID>) = apply { this.hendelser = hendelser }

    fun sykepengegrunnlag(sykepengegrunnlag: Double) = apply { this.sykepengegrunnlag = sykepengegrunnlag }

    fun grunnlagForSykepengegrunnlag(grunnlagForSykepengegrunnlag: Double) =
        apply {
            this.grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag
        }

    fun grunnlagForSykepengegrunnlagPerArbeidsgiver(grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>) =
        apply { this.grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver }

    fun begrensning(begrensning: String) = apply { this.begrensning = begrensning }

    fun inntekt(inntekt: Double) = apply { this.inntekt = inntekt }

    fun vedtakFattetTidspunkt(vedtakFattetTidspunkt: LocalDateTime) = apply { this.vedtakFattetTidspunkt = vedtakFattetTidspunkt }

    fun utbetalingId(utbetalingId: UUID) = apply { this.utbetalingId = utbetalingId }

    fun sykepengegrunnlagsfakta(sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta) =
        apply {
            this.sykepengegrunnlagsfakta = sykepengegrunnlagsfakta.medtags(tagsForSykepengegrunnlagsfakta)
        }

    fun skjønnsfastsattSykepengegrunnlag(skjønnsfastsattSykepengegrunnlag: SkjønnsfastsattSykepengegrunnlag) =
        apply {
            this.skjønnsfastsattSykepengegrunnlag = skjønnsfastsattSykepengegrunnlag
            skjønnsfastsattSykepengegrunnlag.byggVedtak(this)
        }

    fun vedtakBegrunnelse(vedtakBegrunnelse: VedtakBegrunnelse) =
        apply {
            vedtakBegrunnelse.byggVedtak(this)
        }

    fun avviksprosent(avviksprosent: Double) =
        apply {
            this.avviksprosent = avviksprosent
        }

    fun sammenligningsgrunnlag(sammenligningsgrunnlag: Sammenligningsgrunnlag) =
        apply {
            this.sammenligningsgrunnlag = sammenligningsgrunnlag
        }

    fun tags(tags: List<String>) =
        apply {
            this.tags.addAll(tags.filterNot { TAGS_SOM_SKAL_LIGGE_I_SYKEPENGEGRUNNLAGSFAKTA.contains(it) })
            this.tagsForSykepengegrunnlagsfakta.addAll(
                tags.filter {
                    TAGS_SOM_SKAL_LIGGE_I_SYKEPENGEGRUNNLAGSFAKTA.contains(
                        it,
                    )
                },
            )
        }

    fun skjønnsfastsettingData(
        begrunnelseFraMal: String,
        begrunnelseFraFritekst: String,
        begrunnelseFraKonklusjon: String,
        type: Skjønnsfastsettingstype,
        årsak: Skjønnsfastsettingsårsak,
    ) = apply {
        this.skjønnsfastsettingopplysninger =
            VedtakMedSkjønnsvurdering.Skjønnsfastsettingopplysninger(
                begrunnelseFraMal,
                begrunnelseFraFritekst,
                begrunnelseFraKonklusjon,
                type,
                årsak,
            )
    }

    fun vedtakBegrunnelseData(vedtakBegrunnelse: VedtakBegrunnelse) =
        apply {
            this.vedtakBegrunnelse = vedtakBegrunnelse
        }

    fun build(): Sykepengevedtak = buildSykepengevedtak()

    private fun buildSykepengevedtak(): Sykepengevedtak {
        return when (val sykepengegrunnlagsfakta = this.sykepengegrunnlagsfakta) {
            is Sykepengegrunnlagsfakta.Infotrygd -> buildVedtakMedOpphavIInfotrygd(utbetalingId, sykepengegrunnlagsfakta)
            is Sykepengegrunnlagsfakta.Spleis -> {
                when (sykepengegrunnlagsfakta) {
                    is Sykepengegrunnlagsfakta.Spleis.EtterHovedregel ->
                        buildVedtakEtterHovedregel(
                            utbetalingId = utbetalingId,
                            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
                            avviksprosent = avviksprosent,
                            sammenligningsgrunnlag = sammenligningsgrunnlag,
                        )
                    is Sykepengegrunnlagsfakta.Spleis.EtterSkjønn ->
                        buildVedtakEtterSkjønn(
                            utbetalingId = utbetalingId,
                            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
                            avviksprosent = avviksprosent,
                            sammenligningsgrunnlag = sammenligningsgrunnlag,
                        )
                }
            }
        }
    }

    private fun buildVedtakEtterHovedregel(
        utbetalingId: UUID,
        sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta.Spleis.EtterHovedregel,
        avviksprosent: Double,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
    ): Sykepengevedtak.Vedtak {
        return Sykepengevedtak.Vedtak(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = spleisBehandlingId,
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
            tags = tags,
            vedtakBegrunnelse = vedtakBegrunnelse,
            avviksprosent = avviksprosent,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
        )
    }

    private fun buildVedtakEtterSkjønn(
        utbetalingId: UUID,
        sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta.Spleis.EtterSkjønn,
        avviksprosent: Double,
        sammenligningsgrunnlag: Sammenligningsgrunnlag,
    ): VedtakMedSkjønnsvurdering {
        val skjønnsfastsettingopplysninger =
            checkNotNull(skjønnsfastsettingopplysninger) {
                "Forventer å finne opplysninger fra saksbehandler ved bygging av vedtak når sykepengegrunnlaget er fastsatt etter skjønn"
            }

        return VedtakMedSkjønnsvurdering(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = spleisBehandlingId,
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
            skjønnsfastsettingopplysninger = skjønnsfastsettingopplysninger,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
            tags = tags,
            vedtakBegrunnelse = vedtakBegrunnelse,
            avviksprosent = avviksprosent,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
        )
    }

    private fun buildVedtakMedOpphavIInfotrygd(
        utbetalingId: UUID,
        sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta.Infotrygd,
    ): Sykepengevedtak.VedtakMedOpphavIInfotrygd {
        return Sykepengevedtak.VedtakMedOpphavIInfotrygd(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = spleisBehandlingId,
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
            tags = tags,
            vedtakBegrunnelse = vedtakBegrunnelse,
        )
    }

    companion object {
        private val TAGS_SOM_SKAL_LIGGE_I_SYKEPENGEGRUNNLAGSFAKTA = listOf("6GBegrenset")
    }
}
