package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.enums.GraphQLBegrunnelse
import no.nav.helse.spleis.graphql.enums.GraphQLSykdomsdagtype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingsdagType
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateToAnyConverter
import java.time.LocalDate

@Generated
public data class GraphQLDag(
    @get:JsonProperty(value = "begrunnelser")
    public val begrunnelser: List<GraphQLBegrunnelse>? = null,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "dato")
    public val dato: LocalDate,
    @get:JsonProperty(value = "grad")
    public val grad: Double? = null,
    @get:JsonProperty(value = "kilde")
    public val kilde: GraphQLSykdomsdagkilde,
    @get:JsonProperty(value = "sykdomsdagtype")
    public val sykdomsdagtype: GraphQLSykdomsdagtype = GraphQLSykdomsdagtype.__UNKNOWN_VALUE,
    @get:JsonProperty(value = "utbetalingsdagtype")
    public val utbetalingsdagtype: GraphQLUtbetalingsdagType =
        GraphQLUtbetalingsdagType.__UNKNOWN_VALUE,
    @get:JsonProperty(value = "utbetalingsinfo")
    public val utbetalingsinfo: GraphQLUtbetalingsinfo? = null,
)
