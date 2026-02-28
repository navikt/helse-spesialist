package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty

@Generated
public data class GraphQLPensjonsgivendeInntekt(
    @get:JsonProperty(value = "arligBelop")
    public val arligBelop: Double,
    @get:JsonProperty(value = "inntektsar")
    public val inntektsar: Int,
)
