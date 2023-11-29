package no.nav.helse.modell.avviksvurdering

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal class Avviksvurdering(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val skjæringstidspunkt: LocalDate,
    private val oppretttet: LocalDateTime,
    private val avviksprosent: Double,
    private val sammenligningsgrunnlag: Sammenligningsgrunnlag,
    private val beregningsgrunnlag: Beregningsgrunnlag

) {
    internal fun toDto() = AvviksvurderingDto(fødselsnummer, aktørId, skjæringstidspunkt, oppretttet, avviksprosent, sammenligningsgrunnlag.toDto(), beregningsgrunnlag.toDto())
}

internal class Beregningsgrunnlag(private val totalbeløp: Double, private val omregnedeÅrsinntekter: List<OmregnetÅrsinntekt>) {
    internal fun toDto() = BeregningsgrunnlagDto(totalbeløp = totalbeløp, omregnedeÅrsinntekter = omregnedeÅrsinntekter.map { it.toDto() })
}

internal class OmregnetÅrsinntekt(private val arbeidsgiverreferanse: String, private val beløp: Double) {
    internal fun toDto() = OmregnetÅrsinntektDto(arbeidsgiverreferanse = arbeidsgiverreferanse, beløp = beløp)
}

internal class Sammenligningsgrunnlag(private val totalbeløp: Double, private val innraporterteInntekter: List<InnrapportertInntekt>) {
    internal fun toDto() = SammenligningsgrunnlagDto(totalbeløp = totalbeløp, innrapprterteInntekter = innraporterteInntekter.map { it.toDto() })
}


internal class InnrapportertInntekt(private val arbeidsgiverreferanse: String, private val inntekter: List<Inntekt>) {
    internal fun toDto() = InnrapportertInntektDto(arbeidsgiverreferanse = arbeidsgiverreferanse, inntekter = inntekter.map { it.toDto() })
}

internal class Inntekt(private val årMåned: YearMonth, private val beløp: Double) {
    internal fun toDto() = InntektDto(årMåned = årMåned, beløp = beløp)

}
