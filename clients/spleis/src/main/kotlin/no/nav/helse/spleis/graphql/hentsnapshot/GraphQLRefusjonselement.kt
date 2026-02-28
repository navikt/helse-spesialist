package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateConverter
import no.nav.helse.spleis.graphql.scalars.AnyToUUIDConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateToAnyConverter
import no.nav.helse.spleis.graphql.scalars.UUIDToAnyConverter
import java.time.LocalDate
import java.util.UUID

@Generated
public data class GraphQLRefusjonselement(
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "fom")
    public val fom: LocalDate,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "tom")
    public val tom: LocalDate? = null,
    @get:JsonProperty(value = "belop")
    public val belop: Double,
    @JsonSerialize(converter = UUIDToAnyConverter::class)
    @JsonDeserialize(converter = AnyToUUIDConverter::class)
    @get:JsonProperty(value = "meldingsreferanseId")
    public val meldingsreferanseId: UUID,
)
