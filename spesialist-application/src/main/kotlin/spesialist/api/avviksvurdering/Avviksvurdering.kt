package no.nav.helse.spesialist.api.avviksvurdering

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class Avviksvurdering(
    val unikId: UUID,
    val vilkårsgrunnlagId: UUID,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val opprettet: LocalDateTime,
    val avviksprosent: Double,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag,
    val beregningsgrunnlag: Beregningsgrunnlag,
)

data class Sammenligningsgrunnlag(
    val totalbeløp: Double,
    val innrapporterteInntekter: List<InnrapportertInntekt>,
)

data class Inntekt(
    val årMåned: YearMonth,
    val beløp: Double,
)

data class Beregningsgrunnlag(
    val totalbeløp: Double,
    val omregnedeÅrsinntekter: List<OmregnetÅrsinntekt>,
)

data class InnrapportertInntekt(
    val arbeidsgiverreferanse: String,
    val inntekter: List<Inntekt>,
)

data class OmregnetÅrsinntekt(
    val arbeidsgiverreferanse: String,
    val beløp: Double,
)
