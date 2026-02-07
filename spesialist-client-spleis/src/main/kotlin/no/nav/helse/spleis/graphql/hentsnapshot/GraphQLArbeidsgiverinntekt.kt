package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateToAnyConverter
import java.time.LocalDate

@Generated
public data class GraphQLArbeidsgiverinntekt(
    @get:JsonProperty(value = "arbeidsgiver")
    public val arbeidsgiver: String,
    @get:JsonProperty(value = "omregnetArsinntekt")
    public val omregnetArsinntekt: GraphQLOmregnetArsinntekt,
    @get:JsonProperty(value = "skjonnsmessigFastsatt")
    public val skjonnsmessigFastsatt: GraphQLSkjonnsmessigFastsatt? = null,
    @get:JsonProperty(value = "deaktivert")
    public val deaktivert: Boolean? = null,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "fom")
    public val fom: LocalDate,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "tom")
    public val tom: LocalDate? = null,
)
