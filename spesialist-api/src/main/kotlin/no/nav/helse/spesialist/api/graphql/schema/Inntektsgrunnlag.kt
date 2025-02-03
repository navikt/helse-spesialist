package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spleis.graphql.enums.GraphQLInntektskilde
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntekterFraAOrdningen
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLOmregnetArsinntekt
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSkjonnsmessigFastsatt

fun GraphQLOmregnetArsinntekt.tilOmregnetÅrsinntekt(): ApiOmregnetArsinntekt =
    ApiOmregnetArsinntekt(
        belop = belop,
        inntektFraAOrdningen = inntekterFraAOrdningen?.map { it.tilInntektFraAOrdningen() },
        kilde = kilde.tilInntektskilde(),
        manedsbelop = manedsbelop,
    )

fun GraphQLSkjonnsmessigFastsatt.tilOmregnetÅrsinntekt(): ApiOmregnetArsinntekt =
    ApiOmregnetArsinntekt(
        belop = belop,
        inntektFraAOrdningen = null,
        kilde = ApiInntektskilde.SKJONNSMESSIG_FASTSATT,
        manedsbelop = manedsbelop,
    )

private fun GraphQLInntekterFraAOrdningen.tilInntektFraAOrdningen(): ApiInntektFraAOrdningen =
    ApiInntektFraAOrdningen(
        maned = maned,
        sum = sum,
    )

private fun GraphQLInntektskilde.tilInntektskilde(): ApiInntektskilde =
    when (this) {
        GraphQLInntektskilde.AORDNINGEN -> ApiInntektskilde.AORDNINGEN
        GraphQLInntektskilde.INFOTRYGD -> ApiInntektskilde.INFOTRYGD
        GraphQLInntektskilde.INNTEKTSMELDING -> ApiInntektskilde.INNTEKTSMELDING
        GraphQLInntektskilde.SAKSBEHANDLER -> ApiInntektskilde.SAKSBEHANDLER
        GraphQLInntektskilde.IKKERAPPORTERT -> ApiInntektskilde.IKKE_RAPPORTERT
        else -> throw Exception("Ukjent inntektskilde ${this.name}")
    }
