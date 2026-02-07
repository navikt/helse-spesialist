package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.helse.spleis.graphql.enums.GraphQLInntektskilde

@Generated
public data class GraphQLOmregnetArsinntekt(
    @get:JsonProperty(value = "belop")
    public val belop: Double,
    @get:JsonProperty(value = "inntekterFraAOrdningen")
    public val inntekterFraAOrdningen: List<GraphQLInntekterFraAOrdningen>? = null,
    @get:JsonProperty(value = "kilde")
    public val kilde: GraphQLInntektskilde = GraphQLInntektskilde.__UNKNOWN_VALUE,
    @get:JsonProperty(value = "manedsbelop")
    public val manedsbelop: Double,
)
