package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingstatus
import no.nav.helse.spleis.graphql.enums.Utbetalingtype
import no.nav.helse.spleis.graphql.scalars.AnyToUUIDConverter
import no.nav.helse.spleis.graphql.scalars.UUIDToAnyConverter
import java.util.UUID

@Generated
public data class GraphQLUtbetaling(
    @JsonSerialize(converter = UUIDToAnyConverter::class)
    @JsonDeserialize(converter = AnyToUUIDConverter::class)
    @get:JsonProperty(value = "id")
    public val id: UUID,
    @get:JsonProperty(value = "arbeidsgiverFagsystemId")
    public val arbeidsgiverFagsystemId: String,
    @get:JsonProperty(value = "arbeidsgiverNettoBelop")
    public val arbeidsgiverNettoBelop: Int,
    @get:JsonProperty(value = "personFagsystemId")
    public val personFagsystemId: String,
    @get:JsonProperty(value = "personNettoBelop")
    public val personNettoBelop: Int,
    @get:JsonProperty(value = "statusEnum")
    public val statusEnum: GraphQLUtbetalingstatus = GraphQLUtbetalingstatus.__UNKNOWN_VALUE,
    @get:JsonProperty(value = "typeEnum")
    public val typeEnum: Utbetalingtype = Utbetalingtype.__UNKNOWN_VALUE,
    @get:JsonProperty(value = "vurdering")
    public val vurdering: GraphQLVurdering? = null,
    @get:JsonProperty(value = "personoppdrag")
    public val personoppdrag: GraphQLOppdrag? = null,
    @get:JsonProperty(value = "arbeidsgiveroppdrag")
    public val arbeidsgiveroppdrag: GraphQLOppdrag? = null,
)
