package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty

@Generated
public data class GraphQLArbeidsgiver(
    @get:JsonProperty(value = "organisasjonsnummer")
    public val organisasjonsnummer: String,
    @get:JsonProperty(value = "ghostPerioder")
    public val ghostPerioder: List<GraphQLGhostPeriode>,
    @get:JsonProperty(value = "generasjoner")
    public val generasjoner: List<GraphQLGenerasjon>,
)
