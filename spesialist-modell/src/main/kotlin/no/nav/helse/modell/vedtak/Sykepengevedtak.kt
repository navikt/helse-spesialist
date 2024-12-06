package no.nav.helse.modell.vedtak

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed interface Sykepengevedtak {
    val fødselsnummer: String
    val aktørId: String
    val vedtaksperiodeId: UUID
    val spleisBehandlingId: UUID
    val organisasjonsnummer: String
    val fom: LocalDate
    val tom: LocalDate
    val skjæringstidspunkt: LocalDate
    val hendelser: List<UUID>
    val sykepengegrunnlag: Double
    val grunnlagForSykepengegrunnlag: Double
    val grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>
    val begrensning: String
    val inntekt: Double
    val vedtakFattetTidspunkt: LocalDateTime
    val tags: Set<String>

    data class IkkeRealitetsbehandlet(
        override val fødselsnummer: String,
        override val aktørId: String,
        override val vedtaksperiodeId: UUID,
        override val spleisBehandlingId: UUID,
        override val organisasjonsnummer: String,
        override val fom: LocalDate,
        override val tom: LocalDate,
        override val skjæringstidspunkt: LocalDate,
        override val hendelser: List<UUID>,
        override val sykepengegrunnlag: Double,
        override val grunnlagForSykepengegrunnlag: Double,
        override val grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
        override val begrensning: String,
        override val inntekt: Double,
        override val vedtakFattetTidspunkt: LocalDateTime,
        override val tags: Set<String>,
    ) : Sykepengevedtak

    data class Vedtak(
        override val fødselsnummer: String,
        override val aktørId: String,
        override val organisasjonsnummer: String,
        override val vedtaksperiodeId: UUID,
        override val spleisBehandlingId: UUID,
        val utbetalingId: UUID,
        override val fom: LocalDate,
        override val tom: LocalDate,
        override val skjæringstidspunkt: LocalDate,
        override val hendelser: List<UUID>,
        override val sykepengegrunnlag: Double,
        override val grunnlagForSykepengegrunnlag: Double,
        override val grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
        override val begrensning: String,
        override val inntekt: Double,
        val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta.Spleis,
        val skjønnsfastsettingopplysninger: SkjønnsfastsettingopplysningerDto?,
        override val vedtakFattetTidspunkt: LocalDateTime,
        override val tags: Set<String>,
        val vedtakBegrunnelse: VedtakBegrunnelseDto?,
    ) : Sykepengevedtak

    data class VedtakMedOpphavIInfotrygd(
        override val fødselsnummer: String,
        override val aktørId: String,
        override val organisasjonsnummer: String,
        override val vedtaksperiodeId: UUID,
        override val spleisBehandlingId: UUID,
        val utbetalingId: UUID,
        override val fom: LocalDate,
        override val tom: LocalDate,
        override val skjæringstidspunkt: LocalDate,
        override val hendelser: List<UUID>,
        override val sykepengegrunnlag: Double,
        override val grunnlagForSykepengegrunnlag: Double,
        override val grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
        override val begrensning: String,
        override val inntekt: Double,
        val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta.Infotrygd,
        override val vedtakFattetTidspunkt: LocalDateTime,
        override val tags: Set<String>,
        val vedtakBegrunnelse: VedtakBegrunnelseDto?,
    ) : Sykepengevedtak
}

data class SkjønnsfastsettingopplysningerDto(
    val begrunnelseFraMal: String,
    val begrunnelseFraFritekst: String,
    val begrunnelseFraKonklusjon: String,
    val skjønnsfastsettingtype: Skjønnsfastsettingstype,
    val skjønnsfastsettingsårsak: Skjønnsfastsettingsårsak,
)

data class VedtakBegrunnelseDto(
    val utfall: UtfallDto,
    val begrunnelse: String?,
) {
    enum class UtfallDto {
        AVSLAG,
        DELVIS_INNVILGELSE,
        INNVILGELSE,
    }
}
