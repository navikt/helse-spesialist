package no.nav.helse.modell.vedtak

import no.nav.helse.modell.vedtak.Sykepengevedtak.VedtakMedSkjønnsvurdering
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
    private var utbetalingId: UUID? = null
    private var sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta? = null
    private var skjønnsfastsattSykepengegrunnlag: SkjønnsfastsattSykepengegrunnlag? = null
    private var skjønnsfastsettingopplysninger: VedtakMedSkjønnsvurdering.SkjønnsfastsettingopplysningerDto? = null
    private var vedtakBegrunnelse: VedtakBegrunnelseDto? = null
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
            VedtakMedSkjønnsvurdering.SkjønnsfastsettingopplysningerDto(
                begrunnelseFraMal,
                begrunnelseFraFritekst,
                begrunnelseFraKonklusjon,
                type,
                årsak,
            )
    }

    fun vedtakBegrunnelseData(vedtakBegrunnelse: VedtakBegrunnelse) =
        apply {
            this.vedtakBegrunnelse = vedtakBegrunnelse.toDto()
        }

    fun build(): Sykepengevedtak {
        if (utbetalingId != null && sykepengegrunnlagsfakta != null) return buildVedtak()
        return buildIkkeRealitetsbehandlet()
    }

    private fun buildIkkeRealitetsbehandlet(): Sykepengevedtak.IkkeRealitetsbehandlet {
        return Sykepengevedtak.IkkeRealitetsbehandlet(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = spleisBehandlingId,
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
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
            tags = tags,
        )
    }

    private fun buildVedtak(): Sykepengevedtak {
        val sykepengegrunnlagsfakta =
            requireNotNull(sykepengegrunnlagsfakta) { "Forventer å finne sykepengegrunnlagsfakta ved bygging av vedtak" }
        val utbetalingId = requireNotNull(utbetalingId) { "Forventer å finne utbetalingId ved bygging av vedtak" }
        return when (sykepengegrunnlagsfakta) {
            is Sykepengegrunnlagsfakta.Infotrygd -> buildVedtakMedOpphavIInfotrygd(utbetalingId, sykepengegrunnlagsfakta)
            is Sykepengegrunnlagsfakta.Spleis.EtterHovedregel -> buildVedtak(utbetalingId, sykepengegrunnlagsfakta)
            is Sykepengegrunnlagsfakta.Spleis.EtterSkjønn -> buildVedtakEtterSkjønn(utbetalingId, sykepengegrunnlagsfakta)
        }
    }

    private fun buildVedtakEtterSkjønn(
        utbetalingId: UUID,
        sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta.Spleis.EtterSkjønn,
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
        )
    }

    private fun buildVedtak(
        utbetalingId: UUID,
        sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta.Spleis.EtterHovedregel,
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
