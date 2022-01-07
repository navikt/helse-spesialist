package no.nav.helse.mediator.api.graphql.schema

import no.nav.helse.mediator.graphql.LocalDate
import no.nav.helse.mediator.graphql.YearMonth
import no.nav.helse.mediator.graphql.enums.GraphQLInntektskilde
import no.nav.helse.mediator.graphql.hentsnapshot.*

data class Inntektsgrunnlag(
    val avviksprosent: Double?,
    val grunnbelop: Int,
    val inntekter: List<Arbeidsgiverinntekt>,
    val maksUtbetalingPerDag: Double?,
    val omregnetArsinntekt: Double?,
    val oppfyllerKravOmMinstelonn: Boolean?,
    val sammenligningsgrunnlag: Double?,
    val skjaeringstidspunkt: LocalDate,
    val sykepengegrunnlag: Double?
)

data class Arbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetArsinntekt: OmregnetArsinntekt?,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag?
)

data class Sammenligningsgrunnlag(
    val belop: Double,
    val inntektFraAOrdningen: List<InntektFraAOrdningen>
)

data class OmregnetArsinntekt(
    val belop: Double,
    val inntektFraAOrdningen: List<InntektFraAOrdningen>?,
    val kilde: Inntektskilde,
    val manedsbelop: Double
)

data class InntektFraAOrdningen(
    val maned: YearMonth,
    val sum: Double
)

enum class Inntektskilde {
    AORDNINGEN,
    INFOTRYGD,
    INNTEKTSMELDING,
    SAKSBEHANDLER
}

internal fun GraphQLInntektsgrunnlag.tilInntektsgrunnlag(): Inntektsgrunnlag =
    Inntektsgrunnlag(
        avviksprosent = avviksprosent,
        grunnbelop = grunnbelop,
        inntekter = inntekter.map { it.tilArbeidsgiverinntekt() },
        maksUtbetalingPerDag = maksUtbetalingPerDag,
        omregnetArsinntekt = omregnetArsinntekt,
        oppfyllerKravOmMinstelonn = oppfyllerKravOmMinstelonn,
        sammenligningsgrunnlag = sammenligningsgrunnlag,
        skjaeringstidspunkt = skjaeringstidspunkt,
        sykepengegrunnlag = sykepengegrunnlag
    )

internal fun GraphQLArbeidsgiverinntekt.tilArbeidsgiverinntekt(): Arbeidsgiverinntekt =
    Arbeidsgiverinntekt(
        arbeidsgiver = arbeidsgiver,
        omregnetArsinntekt = omregnetArsinntekt?.tilOmregnetÅrsinntekt(),
        sammenligningsgrunnlag = sammenligningsgrunnlag?.tilSammenligningsgrunnlag()
    )

private fun GraphQLSammenligningsgrunnlag.tilSammenligningsgrunnlag(): Sammenligningsgrunnlag =
    Sammenligningsgrunnlag(
        belop = belop,
        inntektFraAOrdningen = inntekterFraAOrdningen.map { it.tilInntektFraAOrdningen() }
    )

private fun GraphQLOmregnetArsinntekt.tilOmregnetÅrsinntekt(): OmregnetArsinntekt =
    OmregnetArsinntekt(
        belop = belop,
        inntektFraAOrdningen = inntekterFraAOrdningen?.map { it.tilInntektFraAOrdningen() },
        kilde = kilde.tilInntektskilde(),
        manedsbelop = manedsbelop
    )

private fun GraphQLInntekterFraAOrdningen.tilInntektFraAOrdningen(): InntektFraAOrdningen =
    InntektFraAOrdningen(
        maned = maned,
        sum = sum
    )

private fun GraphQLInntektskilde.tilInntektskilde(): Inntektskilde =
    when (this) {
        GraphQLInntektskilde.AORDNINGEN -> Inntektskilde.AORDNINGEN
        GraphQLInntektskilde.INFOTRYGD -> Inntektskilde.INFOTRYGD
        GraphQLInntektskilde.INNTEKTSMELDING -> Inntektskilde.INNTEKTSMELDING
        GraphQLInntektskilde.SAKSBEHANDLER -> Inntektskilde.SAKSBEHANDLER
        else -> throw Exception("Ukjent inntektskilde ${this.name}")
    }
