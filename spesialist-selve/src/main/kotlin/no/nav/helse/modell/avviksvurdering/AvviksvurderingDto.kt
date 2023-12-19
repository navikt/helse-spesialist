package no.nav.helse.modell.avviksvurdering

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class AvviksvurderingDto(
    val unikId: UUID,
    val vilkårsgrunnlagId: UUID?,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val opprettet: LocalDateTime,
    val avviksprosent: Double,
    val sammenligningsgrunnlag: SammenligningsgrunnlagDto,
    val beregningsgrunnlag: BeregningsgrunnlagDto,
)

data class SammenligningsgrunnlagDto(
    val totalbeløp: Double,
    val innrapporterteInntekter: List<InnrapportertInntektDto>,
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

