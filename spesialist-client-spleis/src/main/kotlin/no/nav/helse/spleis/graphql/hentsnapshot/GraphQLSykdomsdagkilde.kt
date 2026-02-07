package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.enums.GraphQLSykdomsdagkildetype
import no.nav.helse.spleis.graphql.scalars.AnyToUUIDConverter
import no.nav.helse.spleis.graphql.scalars.UUIDToAnyConverter
import java.util.UUID

@Generated
public data class GraphQLSykdomsdagkilde(
    @JsonSerialize(converter = UUIDToAnyConverter::class)
    @JsonDeserialize(converter = AnyToUUIDConverter::class)
    @get:JsonProperty(value = "id")
    public val id: UUID,
    @get:JsonProperty(value = "type")
    public val type: GraphQLSykdomsdagkildetype = GraphQLSykdomsdagkildetype.__UNKNOWN_VALUE,
)
