package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty

@Generated
public data class GraphQLSimulering(
    @get:JsonProperty(value = "totalbelop")
    public val totalbelop: Int,
    @get:JsonProperty(value = "perioder")
    public val perioder: List<GraphQLSimuleringsperiode>,
)
