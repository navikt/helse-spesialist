package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spleis.graphql.enums.GraphQLInntektskilde
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLArbeidsgiverinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntekterFraAOrdningen
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOmregnetArsinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSammenligningsgrunnlag

data class Arbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetArsinntekt: OmregnetArsinntekt?,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag?,
    val skjonnsmessigFastsatt: OmregnetArsinntekt?,
    val deaktivert: Boolean? = null
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
    val maned: YearMonthString,
    val sum: Double
)

enum class Inntektskilde {
    AORDNINGEN,
    INFOTRYGD,
    INNTEKTSMELDING,
    SAKSBEHANDLER,
    IKKE_RAPPORTERT,
    SKJONNSMESSIG_FASTSATT
}

internal fun GraphQLArbeidsgiverinntekt.tilArbeidsgiverinntekt(): Arbeidsgiverinntekt =
    Arbeidsgiverinntekt(
        arbeidsgiver = arbeidsgiver,
        omregnetArsinntekt = omregnetArsinntekt?.tilOmregnetÅrsinntekt(),
        sammenligningsgrunnlag = sammenligningsgrunnlag?.tilSammenligningsgrunnlag(),
        skjonnsmessigFastsatt = skjonnsmessigFastsatt?.tilOmregnetÅrsinntekt(),
        deaktivert = deaktivert
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
        GraphQLInntektskilde.IKKERAPPORTERT -> Inntektskilde.IKKE_RAPPORTERT
        GraphQLInntektskilde.SKJONNSMESSIGFASTSATT -> Inntektskilde.SKJONNSMESSIG_FASTSATT
        else -> throw Exception("Ukjent inntektskilde ${this.name}")
    }
