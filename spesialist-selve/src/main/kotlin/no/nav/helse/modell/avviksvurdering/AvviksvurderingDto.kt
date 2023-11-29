package no.nav.helse.modell.avviksvurdering

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class AvviksvurderingDto(
    val fødselsnummer: String,
    val aktørId: String,
    val skjæringstidspunkt: LocalDate,
    val oppretttet: LocalDateTime,
    val avviksprosent: Double,
    val sammenligningsgrunnlag: SammenligningsgrunnlagDto,
    val beregningsgrunnlag: BeregningsgrunnlagDto,
)

data class SammenligningsgrunnlagDto(
    val totalbeløp: Double,
    val innrapprterteInntekter: List<InnrapportertInntektDto>,
)

data class InnrapportertInntektDto(
    val arbeidsgiverreferanse: String,
    val inntekter: List<InntektDto>,
)

data class InntektDto(
    val årMåned: YearMonth,
    val beløp: Double,
)

data class BeregningsgrunnlagDto(
    val totalbeløp: Double,
    val omregnedeÅrsinntekter: List<OmregnetÅrsinntektDto>
)

data class OmregnetÅrsinntektDto(
    val arbeidsgiverreferanse: String,
    val beløp: Double,
)

