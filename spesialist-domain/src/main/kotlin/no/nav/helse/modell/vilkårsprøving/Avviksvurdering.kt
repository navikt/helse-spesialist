package no.nav.helse.modell.vilkårsprøving

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class Avviksvurdering(
    val unikId: UUID,
    val vilkårsgrunnlagId: UUID?,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val opprettet: LocalDateTime,
    val avviksprosent: Double,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag,
    val beregningsgrunnlag: Beregningsgrunnlag,
) {
    companion object {
        fun List<Avviksvurdering>.finnRiktigAvviksvurdering(skjæringstidspunkt: LocalDate) = filter { it.skjæringstidspunkt == skjæringstidspunkt }.maxByOrNull { it.opprettet }

        fun ny(
            id: UUID,
            vilkårsgrunnlagId: UUID,
            fødselsnummer: String,
            skjæringstidspunkt: LocalDate,
            opprettet: LocalDateTime,
            avviksprosent: Double,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
            beregningsgrunnlag: Beregningsgrunnlag,
        ) = Avviksvurdering(
            unikId = id,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            avviksprosent = avviksprosent,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            beregningsgrunnlag = beregningsgrunnlag,
        )
    }
}

data class Sammenligningsgrunnlag(
    val totalbeløp: Double,
    val innrapporterteInntekter: List<InnrapportertInntekt>,
) {
    private fun relevanteInntekterFor(arbeidsgiverreferanse: String) = innrapporterteInntekter.filter { it.arbeidsgiverreferanse == arbeidsgiverreferanse }

    fun innrapportertÅrsinntektFor(arbeidsgiverreferanse: String) =
        relevanteInntekterFor(arbeidsgiverreferanse)
            .flatMap {
                it.inntekter
            }.sumOf { it.beløp }
}

data class InnrapportertInntekt(
    val arbeidsgiverreferanse: String,
    val inntekter: List<Inntekt>,
)

data class Inntekt(
    val årMåned: YearMonth,
    val beløp: Double,
)

data class Beregningsgrunnlag(
    val totalbeløp: Double,
    val omregnedeÅrsinntekter: List<OmregnetÅrsinntekt>,
)

data class OmregnetÅrsinntekt(
    val arbeidsgiverreferanse: String,
    val beløp: Double,
)
