package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateToAnyConverter
import java.time.LocalDate

@Generated
public data class GraphQLSimuleringsutbetaling(
    @get:JsonProperty(value = "detaljer")
    public val detaljer: List<GraphQLSimuleringsdetaljer>,
    @get:JsonProperty(value = "feilkonto")
    public val feilkonto: Boolean,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "forfall")
    public val forfall: LocalDate,
    @get:JsonProperty(value = "utbetalesTilId")
    public val utbetalesTilId: String,
    @get:JsonProperty(value = "utbetalesTilNavn")
    public val utbetalesTilNavn: String,
)
