package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.`annotation`.JsonProperty

@Generated
public data class GraphQLPeriodevilkar(
    @get:JsonProperty(value = "alder")
    public val alder: Alder,
    @get:JsonProperty(value = "sykepengedager")
    public val sykepengedager: Sykepengedager,
)
