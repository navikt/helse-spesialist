package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spleis.graphql.enums.GraphQLInntektskilde
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntekterFraAOrdningen
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOmregnetArsinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSkjonnsmessigFastsatt

data class Arbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetArsinntekt: OmregnetArsinntekt?,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag?,
    val skjonnsmessigFastsatt: OmregnetArsinntekt?,
    val deaktivert: Boolean? = null,
)

data class Sammenligningsgrunnlag(
    val belop: Double,
    val inntektFraAOrdningen: List<InntektFraAOrdningen>,
)

data class OmregnetArsinntekt(
    val belop: Double,
    val inntektFraAOrdningen: List<InntektFraAOrdningen>?,
    val kilde: Inntektskilde,
    val manedsbelop: Double,
)

data class InntektFraAOrdningen(
    val maned: YearMonthString,
    val sum: Double,
)

enum class Inntektskilde {
    AORDNINGEN,
    INFOTRYGD,
    INNTEKTSMELDING,
    SAKSBEHANDLER,
    IKKE_RAPPORTERT,
    SKJONNSMESSIG_FASTSATT,
}

internal fun GraphQLOmregnetArsinntekt.tilOmregnetÅrsinntekt(): OmregnetArsinntekt =
    OmregnetArsinntekt(
        belop = belop,
        inntektFraAOrdningen = inntekterFraAOrdningen?.map { it.tilInntektFraAOrdningen() },
        kilde = kilde.tilInntektskilde(),
        manedsbelop = manedsbelop,
    )

internal fun GraphQLSkjonnsmessigFastsatt.tilOmregnetÅrsinntekt(): OmregnetArsinntekt =
    OmregnetArsinntekt(
        belop = belop,
        inntektFraAOrdningen = null,
        kilde = Inntektskilde.SKJONNSMESSIG_FASTSATT,
        manedsbelop = manedsbelop,
    )

private fun GraphQLInntekterFraAOrdningen.tilInntektFraAOrdningen(): InntektFraAOrdningen =
    InntektFraAOrdningen(
        maned = maned,
        sum = sum,
    )

private fun GraphQLInntektskilde.tilInntektskilde(): Inntektskilde =
    when (this) {
        GraphQLInntektskilde.AORDNINGEN -> Inntektskilde.AORDNINGEN
        GraphQLInntektskilde.INFOTRYGD -> Inntektskilde.INFOTRYGD
        GraphQLInntektskilde.INNTEKTSMELDING -> Inntektskilde.INNTEKTSMELDING
        GraphQLInntektskilde.SAKSBEHANDLER -> Inntektskilde.SAKSBEHANDLER
        GraphQLInntektskilde.IKKERAPPORTERT -> Inntektskilde.IKKE_RAPPORTERT
        else -> throw Exception("Ukjent inntektskilde ${this.name}")
    }
