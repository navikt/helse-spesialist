package no.nav.helse.modell.vedtak

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
    private var skjønnsfastsettingopplysninger: SkjønnsfastsettingopplysningerDto? = null
    private var avslag: AvslagDto? = null
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

    fun avslag(avslag: Avslag) =
        apply {
            avslag.byggVedtak(this)
        }

    fun tags(tags: List<String>) =
        apply {
            this.tags.addAll(tags.filterNot { TAGS_SOM_SKAL_LIGGE_I_SYKEPENGEGRUNNLAGSFAKTA.contains(it) })
            this.tagsForSykepengegrunnlagsfakta.addAll(tags.filter { TAGS_SOM_SKAL_LIGGE_I_SYKEPENGEGRUNNLAGSFAKTA.contains(it) })
        }

    fun skjønnsfastsettingData(
        begrunnelseFraMal: String,
        begrunnelseFraFritekst: String,
        begrunnelseFraKonklusjon: String,
        type: Skjønnsfastsettingstype,
        årsak: Skjønnsfastsettingsårsak,
    ) = apply {
        this.skjønnsfastsettingopplysninger =
            SkjønnsfastsettingopplysningerDto(
                begrunnelseFraMal,
                begrunnelseFraFritekst,
                begrunnelseFraKonklusjon,
                type,
                årsak,
            )
    }

    fun avslagData(
        type: Avslagstype,
        begrunnelse: String,
    ) = apply {
        this.avslag =
            AvslagDto(
                when (type) {
                    Avslagstype.AVSLAG -> AvslagstypeDto.AVSLAG
                    Avslagstype.DELVIS_AVSLAG -> AvslagstypeDto.DELVIS_AVSLAG
                },
                begrunnelse,
            )
    }

    fun build(): Sykepengevedtak {
        if (utbetalingId != null && sykepengegrunnlagsfakta != null) return buildVedtak()
        return buildAuuVedtak()
    }

    private fun buildAuuVedtak(): Sykepengevedtak.AuuVedtak {
        return Sykepengevedtak.AuuVedtak(
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

    private fun buildVedtak(): Sykepengevedtak.Vedtak {
        val sykepengegrunnlagsfakta =
            requireNotNull(sykepengegrunnlagsfakta) { "Forventer å finne sykepengegrunnlagsfakta ved bygging av vedtak" }
        val utbetalingId = requireNotNull(utbetalingId) { "Forventer å finne utbetalingId ved bygging av vedtak" }
        if (sykepengegrunnlagsfakta is Sykepengegrunnlagsfakta.Spleis.EtterSkjønn) {
            return buildVedtakEtterSkjønn(sykepengegrunnlagsfakta, utbetalingId)
        }
        return buildVedtak(sykepengegrunnlagsfakta, utbetalingId)
    }

    private fun buildVedtakEtterSkjønn(
        sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
        utbetalingId: UUID,
    ): Sykepengevedtak.Vedtak {
        checkNotNull(skjønnsfastsettingopplysninger) {
            "Forventer å finne opplysninger fra saksbehandler ved bygging av vedtak når sykepengegrunnlaget er fastsatt etter skjønn"
        }

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
            skjønnsfastsettingopplysninger = skjønnsfastsettingopplysninger,
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
            tags = tags,
            avslag = avslag,
        )
    }

    private fun buildVedtak(
        sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
        utbetalingId: UUID,
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
            skjønnsfastsettingopplysninger = null,
            tags = tags,
            avslag = avslag,
        )
    }

    companion object {
        private val TAGS_SOM_SKAL_LIGGE_I_SYKEPENGEGRUNNLAGSFAKTA = listOf("6GBegrenset")
    }
}
