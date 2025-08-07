package no.nav.helse.modell.melding

import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class SykepengevedtakSelvstendigNæringsdrivendeDto(
    val fødselsnummer: String,
    val vedtaksperiodeId: UUID,
    val sykepengegrunnlag: BigDecimal,
    val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val vedtakFattetTidspunkt: Instant,
    val utbetalingId: UUID,
    val vedtakBegrunnelse: VedtakBegrunnelse?,
) : UtgåendeHendelse {
    data class Sykepengegrunnlagsfakta(
        val beregningsgrunnlag: BigDecimal,
        val pensjonsgivendeInntekter: List<PensjonsgivendeInntekt>,
        val erBegrensetTil6G: Boolean,
        val `6G`: BigDecimal,
    )

    data class PensjonsgivendeInntekt(
        val år: Int,
        val inntekt: BigDecimal,
    )
}
