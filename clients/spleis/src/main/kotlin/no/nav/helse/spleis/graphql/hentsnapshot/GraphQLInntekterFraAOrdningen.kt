package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.scalars.AnyToYearMonthConverter
import no.nav.helse.spleis.graphql.scalars.YearMonthToAnyConverter
import java.time.YearMonth

@Generated
public data class GraphQLInntekterFraAOrdningen(
    @JsonSerialize(converter = YearMonthToAnyConverter::class)
    @JsonDeserialize(converter = AnyToYearMonthConverter::class)
    @get:JsonProperty(value = "maned")
    public val maned: YearMonth,
    @get:JsonProperty(value = "sum")
    public val sum: Double,
)
