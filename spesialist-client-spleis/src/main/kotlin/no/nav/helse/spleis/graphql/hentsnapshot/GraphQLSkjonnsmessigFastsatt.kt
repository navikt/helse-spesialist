package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty

@Generated
public data class GraphQLSkjonnsmessigFastsatt(
    @get:JsonProperty(value = "belop")
    public val belop: Double,
    @get:JsonProperty(value = "manedsbelop")
    public val manedsbelop: Double,
)
