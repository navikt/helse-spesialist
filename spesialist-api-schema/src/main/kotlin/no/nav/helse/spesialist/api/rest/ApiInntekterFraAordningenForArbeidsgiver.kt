@file:UseContextualSerialization(
    LocalDate::class,
)

package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import no.nav.helse.spesialist.api.rest.saksbilde.ApiInntektFraAOrdningen
import java.time.LocalDate

@Serializable
data class ApiInntekterFraAordningenForArbeidsgiver(
    val organisasjonsnummer: String,
    val skjaeringstidspunkt: LocalDate,
    val inntekter: List<ApiInntektFraAOrdningen>,
)
