package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateTimeConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateTimeToAnyConverter
import java.time.LocalDateTime

@Generated
public data class GraphQLVurdering(
    @get:JsonProperty(value = "automatisk")
    public val automatisk: Boolean,
    @get:JsonProperty(value = "godkjent")
    public val godkjent: Boolean,
    @get:JsonProperty(value = "ident")
    public val ident: String,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "tidsstempel")
    public val tidsstempel: LocalDateTime,
)
