package no.nav.helse.modell.vedtaksperiode.vedtak

import no.nav.helse.modell.sykefraværstilfelle.Skjønnsfastsettingstype
import no.nav.helse.modell.sykefraværstilfelle.Skjønnsfastsettingsårsak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal sealed class Sykepengevedtak(
    val fødselsnummer: String,
    val aktørId: String,
    val vedtaksperiodeId: UUID,
    val spleisBehandlingId: UUID,
    val organisasjonsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val hendelser: List<UUID>,
    val sykepengegrunnlag: Double,
    val grunnlagForSykepengegrunnlag: Double,
    val grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
    val begrensning: String,
    val inntekt: Double,
    val vedtakFattetTidspunkt: LocalDateTime,
    val tags: Set<String>,
) {
    override fun equals(other: Any?) =
        this === other || (
            other is Sykepengevedtak &&
                fødselsnummer == other.fødselsnummer &&
                aktørId == other.aktørId &&
                vedtaksperiodeId == other.vedtaksperiodeId &&
                spleisBehandlingId == other.spleisBehandlingId &&
                organisasjonsnummer == other.organisasjonsnummer &&
                fom == other.fom &&
                tom == other.tom &&
                skjæringstidspunkt == other.skjæringstidspunkt &&
                hendelser == other.hendelser &&
                sykepengegrunnlag == other.sykepengegrunnlag &&
                grunnlagForSykepengegrunnlag == other.grunnlagForSykepengegrunnlag &&
                grunnlagForSykepengegrunnlagPerArbeidsgiver == other.grunnlagForSykepengegrunnlagPerArbeidsgiver &&
                begrensning == other.begrensning &&
                inntekt == other.inntekt &&
                vedtakFattetTidspunkt.withNano(0) == other.vedtakFattetTidspunkt.withNano(0) &&
                tags == other.tags
        )

    override fun hashCode(): Int {
        var result = fødselsnummer.hashCode()
        result = 31 * result + aktørId.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        result = 31 * result + spleisBehandlingId.hashCode()
        result = 31 * result + organisasjonsnummer.hashCode()
        result = 31 * result + fom.hashCode()
        result = 31 * result + tom.hashCode()
        result = 31 * result + skjæringstidspunkt.hashCode()
        result = 31 * result + hendelser.hashCode()
        result = 31 * result + sykepengegrunnlag.hashCode()
        result = 31 * result + grunnlagForSykepengegrunnlag.hashCode()
        result = 31 * result + grunnlagForSykepengegrunnlagPerArbeidsgiver.hashCode()
        result = 31 * result + begrensning.hashCode()
        result = 31 * result + inntekt.hashCode()
        result = 31 * result + vedtakFattetTidspunkt.hashCode()
        result = 31 * result + tags.hashCode()
        return result
    }

    @Suppress("EqualsOrHashCode")
    internal class AuuVedtak(
        fødselsnummer: String,
        aktørId: String,
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID,
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        hendelser: List<UUID>,
        sykepengegrunnlag: Double,
        grunnlagForSykepengegrunnlag: Double,
        grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
        begrensning: String,
        inntekt: Double,
        vedtakFattetTidspunkt: LocalDateTime,
        tags: Set<String>,
    ) : Sykepengevedtak(
            fødselsnummer,
            aktørId,
            vedtaksperiodeId,
            spleisBehandlingId,
            organisasjonsnummer,
            fom,
            tom,
            skjæringstidspunkt,
            hendelser,
            sykepengegrunnlag,
            grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning,
            inntekt,
            vedtakFattetTidspunkt,
            tags,
        ) {
        override fun equals(other: Any?) = this === other || (super.equals(other) && other is AuuVedtak)
    }

    internal class Vedtak(
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        spleisBehandlingId: UUID,
        val utbetalingId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        hendelser: List<UUID>,
        sykepengegrunnlag: Double,
        grunnlagForSykepengegrunnlag: Double,
        grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
        begrensning: String,
        inntekt: Double,
        val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
        val skjønnsfastsettingopplysninger: SkjønnsfastsettingopplysningerDto?,
        vedtakFattetTidspunkt: LocalDateTime,
        tags: Set<String>,
    ) : Sykepengevedtak(
            fødselsnummer,
            aktørId,
            vedtaksperiodeId,
            spleisBehandlingId,
            organisasjonsnummer,
            fom,
            tom,
            skjæringstidspunkt,
            hendelser,
            sykepengegrunnlag,
            grunnlagForSykepengegrunnlag,
            grunnlagForSykepengegrunnlagPerArbeidsgiver,
            begrensning,
            inntekt,
            vedtakFattetTidspunkt,
            tags,
        ) {
        override fun equals(other: Any?) =
            this === other || (
                super.equals(other) &&
                    other is Vedtak &&
                    this.utbetalingId == other.utbetalingId &&
                    this.sykepengegrunnlagsfakta == other.sykepengegrunnlagsfakta &&
                    this.skjønnsfastsettingopplysninger == other.skjønnsfastsettingopplysninger
            )

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + utbetalingId.hashCode()
            result = 31 * result + sykepengegrunnlagsfakta.hashCode()
            result = 31 * result + skjønnsfastsettingopplysninger.hashCode()
            return result
        }
    }
}

internal data class SkjønnsfastsettingopplysningerDto(
    val begrunnelseFraMal: String,
    val begrunnelseFraFritekst: String,
    val begrunnelseFraKonklusjon: String,
    val skjønnsfastsettingtype: Skjønnsfastsettingstype,
    val skjønnsfastsettingsårsak: Skjønnsfastsettingsårsak,
)
