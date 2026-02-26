package no.nav.helse.spleis.graphql.enums

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.`annotation`.JsonEnumDefaultValue
import com.fasterxml.jackson.`annotation`.JsonProperty

@Generated
public enum class GraphQLPeriodetype {
    @JsonProperty("Forlengelse")
    FORLENGELSE,

    @JsonProperty("Forstegangsbehandling")
    FORSTEGANGSBEHANDLING,

    @JsonProperty("Infotrygdforlengelse")
    INFOTRYGDFORLENGELSE,

    @JsonProperty("OvergangFraIt")
    OVERGANGFRAIT,

    /**
     * This is a default enum value that will be used when attempting to deserialize unknown value.
     */
    @JsonEnumDefaultValue
    @Suppress("ktlint:standard:enum-entry-name-case")
    __UNKNOWN_VALUE,
}
